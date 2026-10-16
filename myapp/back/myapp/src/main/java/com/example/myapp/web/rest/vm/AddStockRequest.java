package com.example.myapp.web.rest.vm;

import com.example.myapp.contants.enumeration.StockMarket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to add a stock to the Stock Center. Only the market and the plain
 * exchange code are accepted — the name and all price fields come from the quote
 * provider, so a client cannot inject them.
 */
public class AddStockRequest {

    @NotNull
    private StockMarket market;

    /**
     * Plain exchange code, e.g. 600519 (SH), 000001 (SZ), 00700 (HK), AAPL (US).
     * Restricted to letters, digits, dot and dash so it is safe to interpolate into
     * a provider's query string.
     */
    @NotNull
    @Size(min = 1, max = 20)
    @Pattern(regexp = "[A-Za-z0-9.\\-]+", message = "Code may only contain letters, digits, dot and dash")
    private String code;

    public StockMarket getMarket() {
        return market;
    }

    public void setMarket(StockMarket market) {
        this.market = market;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
