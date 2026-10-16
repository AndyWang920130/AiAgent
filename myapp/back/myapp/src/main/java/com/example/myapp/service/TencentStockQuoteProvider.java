package com.example.myapp.service;

import com.example.myapp.contants.enumeration.StockMarket;
import com.example.myapp.domain.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Quote provider backed by Tencent's public market data endpoint
 * ({@code https://qt.gtimg.cn/q=sh600519,usAAPL}). It needs no API key, covers the
 * A-share, Hong Kong and US markets, and quotes many symbols in one request, so a
 * "refresh all" costs a single outbound call.
 *
 * <p>The response is a series of {@code v_<symbol>="a~b~c~...";} assignments encoded
 * in GBK, so the body is read as bytes and decoded explicitly. Fields are positional;
 * the indices used here are the ones that are identical across the A-share, HK and US
 * payload layouts.
 */
@Component
public class TencentStockQuoteProvider implements StockQuoteProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TencentStockQuoteProvider.class);

    /** GB18030 is a superset of GBK and decodes these payloads without loss. */
    private static final Charset RESPONSE_CHARSET = Charset.forName("GB18030");

    /** Field positions within the tilde-separated quote payload. */
    private static final int IDX_NAME = 1;
    private static final int IDX_PRICE = 3;
    private static final int IDX_PREVIOUS_CLOSE = 4;
    private static final int IDX_OPEN = 5;
    private static final int IDX_VOLUME = 6;
    private static final int IDX_MARKET_TIME = 30;
    private static final int IDX_CHANGE = 31;
    private static final int IDX_CHANGE_PERCENT = 32;
    private static final int IDX_DAY_HIGH = 33;
    private static final int IDX_DAY_LOW = 34;

    /** Minimum field count needed to read everything up to {@link #IDX_DAY_LOW}. */
    private static final int MIN_FIELDS = IDX_DAY_LOW + 1;

    /** Symbols per request, to keep the query string a sane length. */
    private static final int BATCH_SIZE = 50;

    private final RestClient restClient;

    private final String baseUrl;

    public TencentStockQuoteProvider(
        @Qualifier("stockQuoteRestClient") RestClient restClient,
        @Value("${app.stock.tencent.base-url:https://qt.gtimg.cn}") String baseUrl
    ) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return "tencent";
    }

    @Override
    public Map<Long, StockQuote> fetchQuotes(List<Stock> stocks) {
        Map<Long, StockQuote> results = new HashMap<>();
        if (stocks.isEmpty()) {
            return results;
        }
        for (int from = 0; from < stocks.size(); from += BATCH_SIZE) {
            List<Stock> batch = stocks.subList(from, Math.min(stocks.size(), from + BATCH_SIZE));
            results.putAll(fetchBatch(batch));
        }
        return results;
    }

    private Map<Long, StockQuote> fetchBatch(List<Stock> batch) {
        // Symbol -> stock id. A symbol can repeat only if the same market/code pair were
        // stored twice, which the unique constraint prevents.
        Map<String, Long> idBySymbol = new LinkedHashMap<>();
        for (Stock stock : batch) {
            idBySymbol.put(toSymbol(stock), stock.getId());
        }
        String query = String.join(",", idBySymbol.keySet());

        byte[] raw = restClient.get()
            .uri(baseUrl + "/q=" + query)
            .retrieve()
            .body(byte[].class);
        if (raw == null || raw.length == 0) {
            throw new IllegalStateException("Empty response from Tencent quote API");
        }
        return parse(new String(raw, RESPONSE_CHARSET), idBySymbol);
    }

    /**
     * Build Tencent's symbol for a stock: a market prefix plus the plain code, e.g.
     * {@code sh600519}, {@code hk00700}, {@code usAAPL}.
     */
    static String toSymbol(Stock stock) {
        String code = stock.getCode().trim();
        return switch (stock.getMarket()) {
            case SH -> "sh" + code.toLowerCase(Locale.ROOT);
            case SZ -> "sz" + code.toLowerCase(Locale.ROOT);
            case HK -> "hk" + code.toLowerCase(Locale.ROOT);
            case US -> "us" + code.toUpperCase(Locale.ROOT);
        };
    }

    /**
     * Parse the {@code v_<symbol>="...";} assignments. Entries that are empty or too
     * short to carry a price belong to symbols the provider does not know, and are
     * dropped so the rest of the batch still succeeds.
     */
    private Map<Long, StockQuote> parse(String body, Map<String, Long> idBySymbol) {
        Map<Long, StockQuote> results = new HashMap<>();
        for (String statement : body.split(";")) {
            String trimmed = statement.trim();
            int eq = trimmed.indexOf('=');
            if (!trimmed.startsWith("v_") || eq < 0) {
                continue;
            }
            String symbol = trimmed.substring("v_".length(), eq);
            Long stockId = idBySymbol.get(symbol.toLowerCase(Locale.ROOT));
            if (stockId == null) {
                // Tencent lowercases the market prefix but preserves the code's case,
                // so fall back to matching the symbol exactly as we sent it.
                stockId = idBySymbol.get(symbol);
            }
            if (stockId == null) {
                continue;
            }
            String payload = trimmed.substring(eq + 1).replace("\"", "").trim();
            if (payload.isEmpty()) {
                LOG.debug("Tencent returned no data for symbol {}", symbol);
                continue;
            }
            String[] fields = payload.split("~");
            if (fields.length < MIN_FIELDS) {
                LOG.debug("Tencent payload for {} has only {} fields", symbol, fields.length);
                continue;
            }
            BigDecimal price = decimal(fields[IDX_PRICE]);
            if (price == null) {
                continue;
            }
            StockMarket market = marketOf(symbol);
            results.put(stockId, new StockQuote(
                blankToNull(fields[IDX_NAME]),
                price,
                decimal(fields[IDX_PREVIOUS_CLOSE]),
                decimal(fields[IDX_OPEN]),
                decimal(fields[IDX_DAY_HIGH]),
                decimal(fields[IDX_DAY_LOW]),
                decimal(fields[IDX_CHANGE]),
                decimal(fields[IDX_CHANGE_PERCENT]),
                wholeNumber(fields[IDX_VOLUME]),
                market == null ? null : market.getCurrency(),
                marketTime(fields[IDX_MARKET_TIME])
            ));
        }
        return results;
    }

    /** Recover the market from a symbol's prefix so the currency can be derived. */
    private static StockMarket marketOf(String symbol) {
        if (symbol.length() < 2) {
            return null;
        }
        return switch (symbol.substring(0, 2).toLowerCase(Locale.ROOT)) {
            case "sh" -> StockMarket.SH;
            case "sz" -> StockMarket.SZ;
            case "hk" -> StockMarket.HK;
            case "us" -> StockMarket.US;
            default -> null;
        };
    }

    /**
     * Normalize the provider's market timestamp. A-share and HK payloads use a bare
     * {@code yyyyMMddHHmmss}; US payloads already use {@code yyyy-MM-dd HH:mm:ss}.
     * The value is only ever displayed, so it is reformatted rather than parsed into
     * an instant (the provider reports exchange-local time with no offset).
     */
    private static String marketTime(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        if (text.length() == 14 && text.chars().allMatch(Character::isDigit)) {
            return text.substring(0, 4) + "-" + text.substring(4, 6) + "-" + text.substring(6, 8)
                + " " + text.substring(8, 10) + ":" + text.substring(10, 12) + ":" + text.substring(12, 14);
        }
        return text;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal decimal(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Volume is reported in lots for A-shares and shares for US stocks; stored as sent. */
    private static Long wholeNumber(String value) {
        BigDecimal decimal = decimal(value);
        return decimal == null ? null : decimal.longValue();
    }
}
