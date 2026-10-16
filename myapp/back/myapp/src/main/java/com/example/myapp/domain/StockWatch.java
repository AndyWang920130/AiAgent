package com.example.myapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One entry in a user's stock watch list.
 *
 * <p>The {@link Stock} row it points at is shared by everyone tracking that symbol, so a
 * quote is fetched once and the price history stays continuous. This table is what makes
 * each list private: a user only ever sees the stocks they hold a watch for.
 */
@Entity
@Table(
    name = "twsny_stock_watch",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_twsny_stock_watch_username_stock", columnNames = {"username", "stock_id"})
    },
    indexes = {
        @Index(name = "idx_twsny_stock_watch_username", columnList = "username")
    }
)
public class StockWatch extends AbstractAuditingEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Login of the user this watch belongs to. */
    @NotNull
    @Size(max = 50)
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public StockWatch username(String username) {
        this.username = username;
        return this;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public StockWatch stock(Stock stock) {
        this.stock = stock;
        return this;
    }
}
