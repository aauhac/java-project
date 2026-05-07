package com.tradeagent.portfolio;

import com.tradeagent.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
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
}
