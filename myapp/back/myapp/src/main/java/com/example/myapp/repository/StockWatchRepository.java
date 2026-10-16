package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.StockMarket;
import com.example.myapp.domain.Stock;
import com.example.myapp.domain.StockWatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link StockWatch} entity.
 */
@Repository
public interface StockWatchRepository extends JpaRepository<StockWatch, Long> {

    /**
     * A user's watch list in the order they added it, with the shared stock row joined in
     * so rendering the list doesn't fire a query per row.
     */
    @Query("select w from StockWatch w join fetch w.stock where w.username = :username order by w.id asc")
    List<StockWatch> findByUsernameWithStock(@Param("username") String username);

    Optional<StockWatch> findByUsernameAndStockId(String username, Long stockId);

    boolean existsByUsernameAndStockMarketAndStockCodeIgnoreCase(String username, StockMarket market, String code);

    long countByUsername(String username);

    long countByStockId(Long stockId);

    void deleteByStock(Stock stock);
}
