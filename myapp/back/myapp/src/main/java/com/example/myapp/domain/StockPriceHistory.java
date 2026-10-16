package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One append-only price point for a {@link Stock}, written automatically whenever a
 * quote is fetched and the price (or the provider's market timestamp) has moved. Rows
 * are never updated, so the table is the stock's price history.
 */
@Entity
@Table(
    name = "twsny_stock_price_history",
    indexes = {
        @Index(name = "idx_twsny_stock_price_history_stock_id", columnList = "stock_id"),
        @Index(name = "idx_twsny_stock_price_history_recorded_date", columnList = "recorded_date")
    }
)
public class StockPriceHistory extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @NotNull
    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "price_change", precision = 19, scale = 4)
    private BigDecimal priceChange;

    @Column(name = "change_percent", precision = 19, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "volume")
    private Long volume;

    /** When this app fetched the quote that produced this point. */
    @NotNull
    @Column(name = "recorded_date", nullable = false)
    private Instant recordedDate = Instant.now();

    /** The provider's market timestamp for this point, kept verbatim as text. */
    @Size(max = 50)
    @Column(name = "market_time_text", length = 50)
    private String marketTimeText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
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
        if (!(o instanceof StockPriceHistory)) {
            return false;
        }
        return getId() != null && getId().equals(((StockPriceHistory) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
