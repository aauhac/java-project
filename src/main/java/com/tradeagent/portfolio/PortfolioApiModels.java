package com.tradeagent.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    public record WatchlistRequestDto(
            Long userId,
            String symbol,
            LocalDate watchStartDate
    ) {
    }

    public record WatchlistRemoveRequestDto(
            Long userId,
            List<String> symbols
    ) {
    }

    public record WatchlistDto(
            String symbol,
            LocalDate watchStartDate,
            BigDecimal basePrice,
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