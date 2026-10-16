package com.example.myapp.service;

import com.example.myapp.contants.enumeration.StockMarket;
import com.example.myapp.domain.Stock;
import com.example.myapp.domain.StockPriceHistory;
import com.example.myapp.domain.StockWatch;
import com.example.myapp.repository.StockPriceHistoryRepository;
import com.example.myapp.repository.StockRepository;
import com.example.myapp.repository.StockWatchRepository;
import com.example.myapp.service.dto.StockDTO;
import com.example.myapp.service.dto.StockPriceHistoryDTO;
import com.example.myapp.service.mapper.StockMapper;
import com.example.myapp.service.mapper.StockPriceHistoryMapper;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import com.example.myapp.web.rest.vm.AddStockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stock Center service: keeps a per-user watch list, refreshes the latest price from a
 * public quote API, and appends every new price to the stock's history.
 *
 * <p>Watch lists are private — a user only ever sees, refreshes, or reads the history of a
 * stock they hold a {@link StockWatch} for, and a stock another user is watching is
 * indistinguishable from one that doesn't exist. The underlying {@link Stock} row is still
 * shared per symbol ({@code market} + {@code code} are unique), so a symbol two people
 * follow is fetched once and keeps one continuous history. Removing a stock removes only
 * the caller's watch; the shared row and its history are dropped once nobody is left
 * watching it.
 */
@Service
@Transactional
public class StockService {

    private static final Logger LOG = LoggerFactory.getLogger(StockService.class);

    private static final String ENTITY_NAME = "stock";

    /** Keep the stored error short enough for the column, with room for an ellipsis. */
    private static final int MAX_ERROR_LENGTH = 500;

    private final StockRepository stockRepository;

    private final StockWatchRepository stockWatchRepository;

    private final StockPriceHistoryRepository stockPriceHistoryRepository;

    private final StockMapper stockMapper;

    private final StockPriceHistoryMapper stockPriceHistoryMapper;

    private final List<StockQuoteProvider> providers;

    private final String providerName;

    private final int maxStocks;

    public StockService(
        StockRepository stockRepository,
        StockWatchRepository stockWatchRepository,
        StockPriceHistoryRepository stockPriceHistoryRepository,
        StockMapper stockMapper,
        StockPriceHistoryMapper stockPriceHistoryMapper,
        List<StockQuoteProvider> providers,
        @Value("${app.stock.provider:tencent}") String providerName,
        @Value("${app.stock.max-stocks:200}") int maxStocks
    ) {
        this.stockRepository = stockRepository;
        this.stockWatchRepository = stockWatchRepository;
        this.stockPriceHistoryRepository = stockPriceHistoryRepository;
        this.stockMapper = stockMapper;
        this.stockPriceHistoryMapper = stockPriceHistoryMapper;
        this.providers = providers;
        this.providerName = providerName;
        this.maxStocks = maxStocks;
    }

    /**
     * Add a stock to the caller's watch list and fetch its quote immediately, so the row
     * never shows up blank. A failed first fetch is kept on the row rather than aborting
     * the add — the symbol may simply be closed or mistyped, and refresh can retry.
     *
     * <p>If somebody else already watches the symbol, their row is reused: the caller gets
     * their own watch pointing at it, and inherits the history already recorded for it.
     *
     * @param request the market and plain exchange code to track.
     * @return the newly watched stock with whatever quote data was available.
     */
    public StockDTO create(AddStockRequest request) {
        LOG.debug("Request to add Stock : {} {}", request.getMarket(), request.getCode());
        StockMarket market = request.getMarket();
        String code = normalizeCode(market, request.getCode());
        String username = SecurityUtil.getCurrentUsername();
        if (stockWatchRepository.existsByUsernameAndStockMarketAndStockCodeIgnoreCase(username, market, code)) {
            throw new BadRequestAlertException("Stock already tracked", ENTITY_NAME, "stockexists");
        }
        if (stockWatchRepository.countByUsername(username) >= maxStocks) {
            throw new BadRequestAlertException("Watch list is full", ENTITY_NAME, "toomanystocks");
        }
        Stock stock = stockRepository
            .findByMarketAndCodeIgnoreCase(market, code)
            .orElseGet(() -> stockRepository.save(new Stock().market(market).code(code).currency(market.getCurrency())));

        StockWatch watch = new StockWatch().username(username).stock(stock);
        stockWatchRepository.save(watch);

        refresh(List.of(stock));
        return toDto(watch);
    }

    /**
     * List the caller's watch list without touching the quote API, so page loads stay fast
     * and don't depend on a third party being reachable.
     *
     * @return the stocks this user watches, oldest first.
     */
    @Transactional(readOnly = true)
    public List<StockDTO> findAll() {
        LOG.debug("Request to get all Stocks of the current user");
        return toDto(currentWatches());
    }

    /**
     * Get one watched stock's cached latest quote.
     *
     * @param id the id of the stock.
     * @return the stock, or empty when the caller doesn't watch it.
     */
    @Transactional(readOnly = true)
    public Optional<StockDTO> findOne(Long id) {
        LOG.debug("Request to get Stock : {}", id);
        return currentWatch(id).map(this::toDto);
    }

    /**
     * Refresh every stock the caller watches in one provider call and return the updated list.
     *
     * @return the caller's stocks with their new quotes.
     */
    public List<StockDTO> refreshAll() {
        LOG.debug("Request to refresh all Stocks of the current user");
        List<StockWatch> watches = currentWatches();
        refresh(watches.stream().map(StockWatch::getStock).toList());
        return toDto(watches);
    }

    /**
     * Refresh a single watched stock.
     *
     * @param id the id of the stock.
     * @return the updated stock, or empty when the caller doesn't watch it.
     */
    public Optional<StockDTO> refreshOne(Long id) {
        LOG.debug("Request to refresh Stock : {}", id);
        return currentWatch(id).map(watch -> {
            refresh(List.of(watch.getStock()));
            return toDto(watch);
        });
    }

    /**
     * Read a watched stock's recorded price history, newest first.
     *
     * @param id the id of the stock.
     * @param pageable the pagination information.
     * @return a page of price points.
     */
    @Transactional(readOnly = true)
    public Page<StockPriceHistoryDTO> findHistory(Long id, Pageable pageable) {
        LOG.debug("Request to get price history of Stock : {}", id);
        currentWatch(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return stockPriceHistoryRepository
            .findByStockIdOrderByRecordedDateDescIdDesc(id, pageable)
            .map(stockPriceHistoryMapper::toDto);
    }

    /**
     * Remove a stock from the caller's watch list. Other users keep theirs; the shared row
     * and its price history are deleted only once the last watcher is gone.
     *
     * @param id the id of the stock.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Stock : {}", id);
        StockWatch watch = currentWatch(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean lastWatcher = stockWatchRepository.countByStockId(id) <= 1;
        stockWatchRepository.delete(watch);
        if (lastWatcher) {
            Stock stock = watch.getStock();
            stockPriceHistoryRepository.deleteByStock(stock);
            stockRepository.delete(stock);
        }
    }

    /** The caller's watch list, with the shared stock rows joined in. */
    private List<StockWatch> currentWatches() {
        return stockWatchRepository.findByUsernameWithStock(SecurityUtil.getCurrentUsername());
    }

    /**
     * The caller's watch for one stock. Empty both when no such stock exists and when it
     * belongs to somebody else's list, so callers can answer 404 to either without
     * revealing that another user tracks the symbol.
     */
    private Optional<StockWatch> currentWatch(Long id) {
        return stockWatchRepository.findByUsernameAndStockId(SecurityUtil.getCurrentUsername(), id);
    }

    /**
     * Fetch quotes for the given stocks and persist both the denormalized latest-price
     * columns and a new history point per stock whose price moved.
     *
     * <p>A provider failure is recorded on each affected row and swallowed: a refresh that
     * can't reach the API should leave the last known prices visible, not fail the request.
     */
    private void refresh(List<Stock> stocks) {
        if (stocks.isEmpty()) {
            return;
        }
        StockQuoteProvider provider = resolveProvider();
        Map<Long, StockQuote> quotes;
        try {
            quotes = provider.fetchQuotes(stocks);
        } catch (Exception e) {
            LOG.warn("Quote provider {} failed for {} stock(s): {}", provider.getName(), stocks.size(), e.getMessage());
            Instant now = Instant.now();
            for (Stock stock : stocks) {
                stock.setLastQuoteError(truncate(e.getMessage() == null ? e.toString() : e.getMessage()));
                stock.setLastQuoteTime(now);
                stock.setQuoteProvider(provider.getName());
            }
            stockRepository.saveAll(stocks);
            return;
        }

        Instant now = Instant.now();
        String username = SecurityUtil.getCurrentUsername();
        List<StockPriceHistory> newPoints = new ArrayList<>();
        for (Stock stock : stocks) {
            StockQuote quote = quotes.get(stock.getId());
            if (quote == null || !isSanePrice(quote.price())) {
                stock.setLastQuoteError(truncate("No quote returned for " + stock.getMarket() + " " + stock.getCode()));
                stock.setLastQuoteTime(now);
                stock.setQuoteProvider(provider.getName());
                continue;
            }
            apply(stock, quote, provider.getName(), now);
            stock.setLastModifiedBy(username);
            StockPriceHistory point = buildHistoryPoint(stock, quote, now, username);
            if (point != null) {
                newPoints.add(point);
            }
        }
        stockRepository.saveAll(stocks);
        if (!newPoints.isEmpty()) {
            stockPriceHistoryRepository.saveAll(newPoints);
        }
    }

    /** Copy a quote onto the stock's latest-price cache. */
    private void apply(Stock stock, StockQuote quote, String provider, Instant fetchedAt) {
        if (quote.name() != null) {
            stock.setName(quote.name());
        }
        if (quote.currency() != null) {
            stock.setCurrency(quote.currency());
        }
        stock.setLatestPrice(quote.price());
        stock.setPreviousClose(quote.previousClose());
        stock.setOpenPrice(quote.open());
        stock.setDayHigh(quote.dayHigh());
        stock.setDayLow(quote.dayLow());
        stock.setPriceChange(quote.change());
        stock.setChangePercent(quote.changePercent());
        stock.setVolume(quote.volume());
        stock.setMarketTimeText(quote.marketTimeText());
        stock.setLastQuoteTime(fetchedAt);
        stock.setQuoteProvider(provider);
        stock.setLastQuoteError(null);
    }

    /**
     * Build a history point, or return null when nothing has moved since the last one.
     * Outside trading hours every refresh returns the same closing price, so de-duplicating
     * on price plus the provider's market timestamp keeps the table from growing for free.
     */
    private StockPriceHistory buildHistoryPoint(Stock stock, StockQuote quote, Instant recordedAt, String username) {
        Optional<StockPriceHistory> last = stockPriceHistoryRepository
            .findFirstByStockIdOrderByRecordedDateDescIdDesc(stock.getId());
        if (last.isPresent() && samePoint(last.get(), quote)) {
            return null;
        }
        StockPriceHistory point = new StockPriceHistory();
        point.setStock(stock);
        point.setPrice(quote.price());
        point.setPriceChange(quote.change());
        point.setChangePercent(quote.changePercent());
        point.setVolume(quote.volume());
        point.setRecordedDate(recordedAt);
        point.setMarketTimeText(quote.marketTimeText());
        point.setCreatedBy(username);
        point.setLastModifiedBy(username);
        return point;
    }

    private boolean samePoint(StockPriceHistory last, StockQuote quote) {
        boolean samePrice = last.getPrice() != null
            && quote.price() != null
            && last.getPrice().compareTo(quote.price()) == 0;
        return samePrice && Objects.equals(last.getMarketTimeText(), quote.marketTimeText());
    }

    /** Resolve the configured provider, falling back to the first one registered. */
    private StockQuoteProvider resolveProvider() {
        for (StockQuoteProvider provider : providers) {
            if (provider.getName().equalsIgnoreCase(providerName)) {
                return provider;
            }
        }
        if (providers.isEmpty()) {
            throw new IllegalStateException("No StockQuoteProvider is registered");
        }
        LOG.warn("Unknown stock provider '{}', falling back to '{}'", providerName, providers.get(0).getName());
        return providers.get(0);
    }

    private List<StockDTO> toDto(List<StockWatch> watches) {
        List<StockDTO> dtos = new ArrayList<>(watches.size());
        for (StockWatch watch : watches) {
            dtos.add(toDto(watch));
        }
        return dtos;
    }

    /**
     * Map the shared stock row, then overlay what belongs to this user's watch: the date
     * they started following the symbol rather than the date it was first added by anyone.
     */
    private StockDTO toDto(StockWatch watch) {
        Stock stock = watch.getStock();
        StockDTO dto = stockMapper.toDto(stock);
        dto.setHistoryCount(stockPriceHistoryRepository.countByStockId(stock.getId()));
        dto.setAddedDate(watch.getCreatedDate());
        return dto;
    }

    /**
     * Normalize the code to the form the providers expect: upper-case US tickers, and
     * Hong Kong codes zero-padded to five digits so {@code 700} and {@code 00700} are
     * the same stock rather than two rows.
     */
    private String normalizeCode(StockMarket market, String code) {
        String trimmed = code == null ? "" : code.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestAlertException("Code is required", ENTITY_NAME, "coderequired");
        }
        return switch (market) {
            case US -> trimmed.toUpperCase(Locale.ROOT);
            case HK -> padLeft(trimmed, 5);
            case SH, SZ -> trimmed;
        };
    }

    private static String padLeft(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        return "0".repeat(length - value.length()) + value;
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH - 3) + "...";
    }

    /**
     * A suspended or unknown symbol comes back with a zero price rather than an error,
     * which would otherwise be written to the history as a real price point.
     */
    private static boolean isSanePrice(BigDecimal price) {
        return price != null && price.signum() > 0;
    }
}
