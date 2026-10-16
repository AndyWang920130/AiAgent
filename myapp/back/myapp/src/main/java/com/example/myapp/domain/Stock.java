package com.example.myapp.domain;

import com.example.myapp.contants.enumeration.StockMarket;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A stock tracked by the Stock Center. {@link #market} plus {@link #code} are the
 * stable identity (unique together); the latest-quote columns are a denormalized
 * cache of the most recent successful fetch so the list renders without replaying
 * {@link StockPriceHistory}.
 *
 * <p>One row per symbol, shared by every user following it, so a quote is fetched once
 * however many watch lists it appears in. Who sees it is decided by
 * {@link StockWatch}, not by this row's {@code createdBy}.
 */
@Entity
@Table(
    name = "twsny_stock",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_twsny_stock_market_code", columnNames = {"market", "code"})
    }
)
public class Stock extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "market", nullable = false, length = 10)
    private StockMarket market;

    /** Plain exchange code without any provider prefix or suffix, e.g. 600519, 00700, AAPL. */
    @NotNull
    @Size(max = 20)
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    /** Display name, filled from the quote provider on the first successful fetch. */
    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;

    @Size(max = 10)
    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "latest_price", precision = 19, scale = 4)
    private BigDecimal latestPrice;

    @Column(name = "previous_close", precision = 19, scale = 4)
    private BigDecimal previousClose;

    @Column(name = "open_price", precision = 19, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "day_high", precision = 19, scale = 4)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 19, scale = 4)
    private BigDecimal dayLow;

    @Column(name = "price_change", precision = 19, scale = 4)
    private BigDecimal priceChange;

    @Column(name = "change_percent", precision = 19, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "volume")
    private Long volume;

    /** When this app fetched the quote (server clock) — not the exchange's own timestamp. */
    @Column(name = "last_quote_time")
    private Instant lastQuoteTime;

    /**
     * The provider's own market timestamp, kept verbatim as text. Providers report it
     * in exchange-local time without an offset, so it is displayed rather than parsed.
     */
    @Size(max = 50)
    @Column(name = "market_time_text", length = 50)
    private String marketTimeText;

    /** Error from the most recent fetch attempt, or null when it succeeded. */
    @Size(max = 500)
    @Column(name = "last_quote_error", length = 500)
    private String lastQuoteError;

    /** Which provider produced the latest quote, e.g. tencent or yahoo. */
    @Size(max = 20)
    @Column(name = "quote_provider", length = 20)
    private String quoteProvider;

    public Long getId() {
        return id;
    }

    public Stock id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockMarket getMarket() {
        return market;
    }

    public Stock market(StockMarket market) {
        this.setMarket(market);
        return this;
    }

    public void setMarket(StockMarket market) {
        this.market = market;
    }

    public String getCode() {
        return code;
    }

    public Stock code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public Stock name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public Stock currency(String currency) {
        this.setCurrency(currency);
        return this;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public Stock latestPrice(BigDecimal latestPrice) {
        this.setLatestPrice(latestPrice);
        return this;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getDayHigh() {
        return dayHigh;
    }

    public void setDayHigh(BigDecimal dayHigh) {
        this.dayHigh = dayHigh;
    }

    public BigDecimal getDayLow() {
        return dayLow;
    }

    public void setDayLow(BigDecimal dayLow) {
        this.dayLow = dayLow;
    }

    public BigDecimal getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(BigDecimal priceChange) {
        this.priceChange = priceChange;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Instant getLastQuoteTime() {
        return lastQuoteTime;
    }

    public void setLastQuoteTime(Instant lastQuoteTime) {
        this.lastQuoteTime = lastQuoteTime;
    }

    public String getMarketTimeText() {
        return marketTimeText;
    }

    public void setMarketTimeText(String marketTimeText) {
        this.marketTimeText = marketTimeText;
    }

    public String getLastQuoteError() {
        return lastQuoteError;
    }

    public void setLastQuoteError(String lastQuoteError) {
        this.lastQuoteError = lastQuoteError;
    }

    public String getQuoteProvider() {
        return quoteProvider;
    }

    public void setQuoteProvider(String quoteProvider) {
        this.quoteProvider = quoteProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Stock)) {
            return false;
        }
        return getId() != null && getId().equals(((Stock) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
