package com.tradeagent.portfolio;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.portfolio.PortfolioApiModels.PortfolioSummaryDto;
import com.tradeagent.portfolio.PortfolioApiModels.PositionDto;
import com.tradeagent.portfolio.PortfolioApiModels.SectorAllocationDto;
import com.tradeagent.portfolio.PortfolioApiModels.TradeHistoryDto;
import com.tradeagent.portfolio.PortfolioApiModels.TradeRequestDto;
import com.tradeagent.portfolio.PortfolioApiModels.WatchlistDto;
import com.tradeagent.portfolio.PortfolioApiModels.WatchlistRemoveRequestDto;
import com.tradeagent.portfolio.PortfolioApiModels.WatchlistRequestDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final WatchlistService watchlistService;

    public PortfolioController(PortfolioService portfolioService,
                               WatchlistService watchlistService) {
        this.portfolioService = portfolioService;
        this.watchlistService = watchlistService;
    }

    @PostMapping("/buy")
    public ApiResponse<PositionDto> buy(@RequestBody TradeRequestDto request) {
        return ApiResponse.ok(portfolioService.buyStock(request));
    }

    @PostMapping("/sell")
    public ApiResponse<Void> sell(@RequestBody TradeRequestDto request) {
        portfolioService.sellStock(request);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{userId}/positions")
    public ApiResponse<List<PositionDto>> getPositions(@PathVariable Long userId) {
        return ApiResponse.ok(portfolioService.getPositions(userId));
    }

    @GetMapping("/{userId}/summary")
    public ApiResponse<PortfolioSummaryDto> getSummary(@PathVariable Long userId) {
        return ApiResponse.ok(portfolioService.getSummary(userId));
    }

    @GetMapping("/{userId}/sector-allocation")
    public ApiResponse<List<SectorAllocationDto>> getSectorAllocation(@PathVariable Long userId) {
        return ApiResponse.ok(portfolioService.getSectorAllocation(userId));
    }

    @GetMapping("/{userId}/trades")
    public ApiResponse<List<TradeHistoryDto>> getTradeHistory(@PathVariable Long userId) {
        return ApiResponse.ok(portfolioService.getTradeHistory(userId));
    }

    @GetMapping("/{userId}/watchlist")
    public ApiResponse<List<WatchlistDto>> getWatchlist(@PathVariable Long userId) {
        return ApiResponse.ok(watchlistService.getWatchlist(userId));
    }

    @PostMapping("/watchlist")
    public ApiResponse<WatchlistDto> addWatchlist(@RequestBody WatchlistRequestDto request) {
        return ApiResponse.ok(watchlistService.addWatchlist(
                request.userId(),
                request.symbol(),
                request.watchStartDate()
        ));
    }

    @PostMapping("/watchlist/remove")
    public ApiResponse<Void> removeWatchlistItems(@RequestBody WatchlistRemoveRequestDto request) {
        watchlistService.removeWatchlistItems(request.userId(), request.symbols());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{userId}/watchlist/{symbol}")
    public ApiResponse<Void> removeWatchlist(@PathVariable Long userId,
                                             @PathVariable String symbol) {
        watchlistService.removeWatchlist(userId, symbol);
        return ApiResponse.ok(null);
    }
}