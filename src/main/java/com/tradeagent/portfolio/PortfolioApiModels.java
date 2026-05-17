package com.tradeagent.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PortfolioApiModels {

    private PortfolioApiModels() {
    }

    public record PortfolioSummaryDto(
            BigDecimal totalBuyAmount,
            BigDecimal totalMarketValue,
            BigDecimal totalProfitLoss,
            BigDecimal totalReturnRate,
            Integer positionCount
    ) {
    }

    public record PositionDto(
            String symbol,
            String sectorCode,
            BigDecimal avgBuyPrice,
            BigDecimal currentPrice,
            Integer quantity,
            BigDecimal marketValue,
            BigDecimal profitLoss,
            BigDecimal returnRate
    ) {
    }

    public record SectorAllocationDto(
            String sectorCode,
            String sectorName,
            BigDecimal marketValue,
            BigDecimal allocationRate
    ) {
    }

    public record TradeRequestDto(
            Long userId,
            String symbol,
            String sectorCode,
            BigDecimal price,
            Integer quantity
    ) {
    }

    public record WatchlistDto(
            String symbol,
            String sectorCode,
            BigDecimal currentPrice,
            BigDecimal changeRate
    ) {
    }

    public record TradeHistoryDto(
            String symbol,
            String sectorCode,
            String tradeType,
            BigDecimal price,
            Integer quantity,
            LocalDateTime tradedAt
    ) {
    }
}
