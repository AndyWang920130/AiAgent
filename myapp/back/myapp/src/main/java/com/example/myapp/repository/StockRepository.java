package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.StockMarket;
import com.example.myapp.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Stock entity.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    /** Reuses the existing symbol row when a second user starts watching it. */
    Optional<Stock> findByMarketAndCodeIgnoreCase(StockMarket market, String code);
}
