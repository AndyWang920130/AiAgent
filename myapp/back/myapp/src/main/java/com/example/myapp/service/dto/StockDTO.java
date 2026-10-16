package com.example.myapp.service.dto;

import com.example.myapp.contants.enumeration.StockMarket;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.example.myapp.domain.Stock} entity, carrying the cached
 * latest quote so the stock list renders in one request.
 */
public class StockDTO implements Serializable {

    private Long id;

    private StockMarket market;

    private String code;

    private String name;

    private String currency;

    private BigDecimal latestPrice;

    private BigDecimal previousClose;

    private BigDecimal openPrice;

    private BigDecimal dayHigh;

    private BigDecimal dayLow;

    private BigDecimal priceChange;

    private BigDecimal changePercent;

    private Long volume;

    private Instant lastQuoteTime;

    private String marketTimeText;

    private String lastQuoteError;

    private String quoteProvider;

    /** How many price-history points this stock has. */
    private Long historyCount;

    /**
     * When the current user added the stock to their own watch list. The shared stock row
     * may be older — another user may have started following the symbol first — and who
     * that was isn't exposed, since watch lists are private.
     */
    private Instant addedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockMarket getMarket() {
        return market;
    }

    public void setMarket(StockMarket market) {
        this.market = market;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
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

    public Long getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(Long historyCount) {
        this.historyCount = historyCount;
    }

    public Instant getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Instant addedDate) {
        this.addedDate = addedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockDTO)) {
            return false;
        }
        return id != null && Objects.equals(id, ((StockDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
