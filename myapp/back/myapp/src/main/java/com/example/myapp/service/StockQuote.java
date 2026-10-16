package com.example.myapp.service;

import java.math.BigDecimal;

/**
 * A single quote as returned by a {@link StockQuoteProvider}. Every field except
 * {@link #price} may be null — providers differ in what they report, and the
 * Stock Center only requires a price to record a history point.
 *
 * @param name           display name reported by the provider
 * @param price          latest traded price
 * @param previousClose  previous session's closing price
 * @param open           current session's opening price
 * @param dayHigh        highest price of the session
 * @param dayLow         lowest price of the session
 * @param change         absolute change against the previous close
 * @param changePercent  percentage change against the previous close
 * @param volume         traded volume in whatever unit the provider reports
 * @param currency       ISO currency code of the price
 * @param marketTimeText the provider's own market timestamp, kept verbatim
 */
public record StockQuote(
    String name,
    BigDecimal price,
    BigDecimal previousClose,
    BigDecimal open,
    BigDecimal dayHigh,
    BigDecimal dayLow,
    BigDecimal change,
    BigDecimal changePercent,
    Long volume,
    String currency,
    String marketTimeText
) {}
