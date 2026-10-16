package com.example.myapp.contants.enumeration;

/**
 * Supported stock markets. The market plus the plain exchange code identify a
 * stock; each quote provider turns that pair into its own symbol format, so no
 * provider-specific string is stored on the entity.
 */
public enum StockMarket {

    /** Shanghai Stock Exchange (A shares), priced in CNY. */
    SH("CNY"),

    /** Shenzhen Stock Exchange (A shares), priced in CNY. */
    SZ("CNY"),

    /** Hong Kong Stock Exchange, priced in HKD. */
    HK("HKD"),

    /** US markets (NYSE, Nasdaq), priced in USD. */
    US("USD");

    private final String currency;

    StockMarket(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }
}
