package com.example.myapp.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.example.myapp.domain.StockPriceHistory} entity — one
 * recorded price point of a stock.
 */
public class StockPriceHistoryDTO implements Serializable {

    private Long id;

    private Long stockId;

    private BigDecimal price;

    private BigDecimal priceChange;

    private BigDecimal changePercent;

    private Long volume;

    private Instant recordedDate;

    private String marketTimeText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public Instant getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(Instant recordedDate) {
        this.recordedDate = recordedDate;
    }

    public String getMarketTimeText() {
        return marketTimeText;
    }

    public void setMarketTimeText(String marketTimeText) {
        this.marketTimeText = marketTimeText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockPriceHistoryDTO)) {
            return false;
        }
        return id != null && Objects.equals(id, ((StockPriceHistoryDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
