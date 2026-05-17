package com.tradeagent.portfolio;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.portfolio.PortfolioApiModels.WatchlistDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping("/{userId}")
    public ApiResponse<WatchlistDto> addWatchlist(@PathVariable Long userId,
                                                  @RequestParam String symbol,
                                                  @RequestParam String sectorCode) {
        return ApiResponse.ok(watchlistService.addWatchlist(userId, symbol, sectorCode));
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<WatchlistDto>> getWatchlist(@PathVariable Long userId) {
        return ApiResponse.ok(watchlistService.getWatchlist(userId));
    }

    @DeleteMapping("/{userId}/{symbol}")
    public ApiResponse<Void> removeWatchlist(@PathVariable Long userId, @PathVariable String symbol) {
        watchlistService.removeWatchlist(userId, symbol);
        return ApiResponse.ok(null);
    }
}
