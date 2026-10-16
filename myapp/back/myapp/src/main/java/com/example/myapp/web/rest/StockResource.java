package com.example.myapp.web.rest;

import com.example.myapp.service.StockService;
import com.example.myapp.service.dto.StockDTO;
import com.example.myapp.service.dto.StockPriceHistoryDTO;
import com.example.myapp.utils.PageUtils;
import com.example.myapp.web.rest.vm.AddStockRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller for the Stock Center. Every endpoint requires an authenticated user
 * (the {@code anyRequest().authenticated()} fallthrough in SecurityConfig) and acts only
 * on that user's watch list: an id somebody else watches answers 404 like any unknown id.
 */
@RestController
@RequestMapping("/api/v1")
public class StockResource {

    private static final Logger LOG = LoggerFactory.getLogger(StockResource.class);

    private final StockService stockService;

    public StockResource(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * GET /stocks : List the current user's tracked stocks with their last known price.
     *
     * <p>Serves the cached quote so the page doesn't wait on the upstream API; use
     * {@code POST /stocks/refresh} to pull fresh prices.
     */
    @GetMapping("/stocks")
    public ResponseEntity<List<StockDTO>> getAllStocks() {
        LOG.debug("REST request to list Stocks");
        return ResponseEntity.ok(stockService.findAll());
    }

    /**
     * GET /stocks/{id} : Get one tracked stock with its last known price.
     */
    @GetMapping("/stocks/{id}")
    public ResponseEntity<StockDTO> getStock(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Stock : {}", id);
        return stockService.findOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /stocks : Add a stock to the current user's list, fetching its quote right away.
     */
    @PostMapping("/stocks")
    public ResponseEntity<StockDTO> addStock(@Valid @RequestBody AddStockRequest request) {
        LOG.debug("REST request to add Stock : {}", request);
        StockDTO result = stockService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/stocks/" + result.getId())).body(result);
    }

    /**
     * POST /stocks/refresh : Pull the latest price for every stock the current user tracks
     * and record it in each stock's price history.
     */
    @PostMapping("/stocks/refresh")
    public ResponseEntity<List<StockDTO>> refreshAllStocks() {
        LOG.debug("REST request to refresh all Stocks");
        return ResponseEntity.ok(stockService.refreshAll());
    }

    /**
     * POST /stocks/{id}/refresh : Pull the latest price for one stock and record it.
     */
    @PostMapping("/stocks/{id}/refresh")
    public ResponseEntity<StockDTO> refreshStock(@PathVariable("id") Long id) {
        LOG.debug("REST request to refresh Stock : {}", id);
        return stockService.refreshOne(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /stocks/{id}/history : Page through a stock's recorded prices, newest first.
     */
    @GetMapping("/stocks/{id}/history")
    public ResponseEntity<List<StockPriceHistoryDTO>> getStockHistory(
        @PathVariable("id") Long id,
        Pageable pageable
    ) {
        LOG.debug("REST request to get price history of Stock : {}", id);
        Page<StockPriceHistoryDTO> page = stockService.findHistory(id, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * DELETE /stocks/{id} : Remove a stock from the current user's list. Other users keep
     * theirs; the price history goes away with the last watcher.
     */
    @DeleteMapping("/stocks/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Stock : {}", id);
        stockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
