package com.example.myapp.service;

import com.example.myapp.domain.Stock;

import java.util.List;
import java.util.Map;

/**
 * Fetches latest prices for tracked stocks from a public quote API. Implementations
 * are selected by {@code app.stock.provider} (see {@link StockService}).
 */
public interface StockQuoteProvider {

    /**
     * Identifier matched against {@code app.stock.provider}, e.g. {@code tencent}.
     * Also stored on the stock so the UI can show where a price came from.
     */
    String getName();

    /**
     * Fetch quotes for the given stocks.
     *
     * <p>Returns results keyed by {@link Stock#getId()}. Stocks the provider could
     * not resolve are simply absent from the map rather than reported as errors, so
     * one bad symbol does not fail the rest of a batch. Implementations throw only
     * when the request itself fails (network error, non-2xx, unparseable payload).
     *
     * @param stocks stocks to quote; never null, may be empty
     * @return quotes by stock id, possibly smaller than the input
     */
    Map<Long, StockQuote> fetchQuotes(List<Stock> stocks);
}
