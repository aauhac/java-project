package com.tradeagent.market;

import com.tradeagent.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/{symbol}/quote")
    public ApiResponse<LatestQuote> getLatestQuote(@PathVariable String symbol) {
        return ApiResponse.ok(marketDataService.getLatestQuote(symbol));
    }

    @GetMapping("/{symbol}/bars")
    public ApiResponse<List<PriceBar>> getHistoricalBars(
            @PathVariable String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(marketDataService.getHistoricalBars(symbol, from, to));
    }
}
