package com.example.myapp.repository;

import com.example.myapp.domain.Stock;
import com.example.myapp.domain.StockPriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the StockPriceHistory entity.
 */
@Repository
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {

    Page<StockPriceHistory> findByStockIdOrderByRecordedDateDescIdDesc(Long stockId, Pageable pageable);

    Optional<StockPriceHistory> findFirstByStockIdOrderByRecordedDateDescIdDesc(Long stockId);

    long countByStockId(Long stockId);

    void deleteByStock(Stock stock);
}
