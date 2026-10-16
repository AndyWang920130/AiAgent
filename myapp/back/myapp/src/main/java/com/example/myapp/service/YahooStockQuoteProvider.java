package com.example.myapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.myapp.domain.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Quote provider backed by Yahoo Finance's public chart endpoint
 * ({@code https://query1.finance.yahoo.com/v8/finance/chart/AAPL}).
 *
 * <p>Kept as an alternative to {@link TencentStockQuoteProvider} for deployments outside
 * mainland China. Yahoo's batch quote endpoint now requires a crumb/cookie handshake, so
 * this provider issues one request per symbol; a failure on a single symbol is recorded as
 * a miss instead of failing the whole refresh.
 */
@Component
public class YahooStockQuoteProvider implements StockQuoteProvider {

    private static final Logger LOG = LoggerFactory.getLogger(YahooStockQuoteProvider.class);

    /** Yahoo rejects requests without a browser-like User-Agent. */
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private static final DateTimeFormatter MARKET_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String baseUrl;

    public YahooStockQuoteProvider(
        @Qualifier("stockQuoteRestClient") RestClient restClient,
        ObjectMapper objectMapper,
        @Value("${app.stock.yahoo.base-url:https://query1.finance.yahoo.com}") String baseUrl
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getName() {
        return "yahoo";
    }

    @Override
    public Map<Long, StockQuote> fetchQuotes(List<Stock> stocks) {
        Map<Long, StockQuote> results = new HashMap<>();
        for (Stock stock : stocks) {
            try {
                StockQuote quote = fetchOne(stock);
                if (quote != null) {
                    results.put(stock.getId(), quote);
                }
            } catch (Exception e) {
                LOG.warn("Yahoo quote failed for {}: {}", toSymbol(stock), e.getMessage());
            }
        }
        return results;
    }

    private StockQuote fetchOne(Stock stock) throws Exception {
        String body = restClient.get()
            .uri(baseUrl + "/v8/finance/chart/" + toSymbol(stock) + "?interval=1d&range=1d")
            .header("User-Agent", USER_AGENT)
            .retrieve()
            .body(String.class);
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode result = objectMapper.readTree(body).path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) {
            return null;
        }
        JsonNode meta = result.get(0).path("meta");
        BigDecimal price = decimal(meta, "regularMarketPrice");
        if (price == null) {
            return null;
        }
        BigDecimal previousClose = decimal(meta, "chartPreviousClose");
        if (previousClose == null) {
            previousClose = decimal(meta, "previousClose");
        }
        BigDecimal change = null;
        BigDecimal changePercent = null;
        if (previousClose != null && previousClose.signum() != 0) {
            change = price.subtract(previousClose);
            changePercent = change.multiply(BigDecimal.valueOf(100))
                .divide(previousClose, 2, RoundingMode.HALF_UP);
        }
        return new StockQuote(
            text(meta, "longName", text(meta, "shortName", null)),
            price,
            previousClose,
            decimal(meta, "regularMarketOpen"),
            decimal(meta, "regularMarketDayHigh"),
            decimal(meta, "regularMarketDayLow"),
            change,
            changePercent,
            meta.path("regularMarketVolume").isNumber() ? meta.path("regularMarketVolume").asLong() : null,
            text(meta, "currency", stock.getMarket().getCurrency()),
            marketTime(meta)
        );
    }

    /**
     * Build Yahoo's symbol: Shanghai is {@code .SS}, Shenzhen {@code .SZ}, Hong Kong a
     * four-digit code with {@code .HK}, and US tickers are used bare.
     */
    static String toSymbol(Stock stock) {
        String code = stock.getCode().trim();
        return switch (stock.getMarket()) {
            case SH -> code + ".SS";
            case SZ -> code + ".SZ";
            case HK -> hongKongCode(code) + ".HK";
            case US -> code.toUpperCase(Locale.ROOT);
        };
    }

    /** Tencent uses five-digit HK codes ({@code 00700}); Yahoo expects four ({@code 0700}). */
    private static String hongKongCode(String code) {
        String stripped = code.replaceFirst("^0+", "");
        if (stripped.isEmpty()) {
            stripped = "0";
        }
        return stripped.length() >= 4 ? stripped : "0".repeat(4 - stripped.length()) + stripped;
    }

    /** Render {@code regularMarketTime} in the exchange's own timezone, for display only. */
    private static String marketTime(JsonNode meta) {
        JsonNode epoch = meta.path("regularMarketTime");
        if (!epoch.isNumber()) {
            return null;
        }
        String zone = meta.path("exchangeTimezoneName").asText("UTC");
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zone);
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }
        return MARKET_TIME.format(Instant.ofEpochSecond(epoch.asLong()).atZone(zoneId));
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.decimalValue() : null;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        if (value.isTextual() && !value.asText().isBlank()) {
            return value.asText();
        }
        return fallback;
    }
}
