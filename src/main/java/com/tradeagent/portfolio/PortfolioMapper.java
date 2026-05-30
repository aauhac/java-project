package com.tradeagent.portfolio;

import com.tradeagent.market.LatestQuote;
import com.tradeagent.portfolio.PortfolioApiModels.PortfolioSummaryDto;
import com.tradeagent.portfolio.PortfolioApiModels.PositionDto;
import com.tradeagent.portfolio.PortfolioApiModels.SectorAllocationDto;
import com.tradeagent.portfolio.PortfolioApiModels.TradeHistoryDto;
import com.tradeagent.portfolio.PortfolioApiModels.WatchlistDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class PortfolioMapper {

    private static final Map<String, String> SECTOR_NAMES = Map.ofEntries(
            Map.entry("SEMI", "Semiconductor"),
            Map.entry("AI", "AI"),
            Map.entry("AI_INFRA", "AI Infrastructure"),
            Map.entry("AIINF", "AI 인프라"),
            Map.entry("EV", "Electric Vehicle"),
            Map.entry("BIO", "Biotech"),
            Map.entry("CLOUD", "Cloud"),
            Map.entry("ENERGY", "Energy"),
            Map.entry("FIN", "Financial"),
            Map.entry("TECH", "Technology")
    );

    public PositionDto toPositionDto(PortfolioPosition position, LatestQuote latestQuote) {
        BigDecimal currentPrice = latestQuote.getLastPrice().setScale(4, RoundingMode.HALF_UP);
        BigDecimal marketValue = position.calculateMarketValue(currentPrice);
        BigDecimal profitLoss = position.calculateProfitLoss(currentPrice);
        BigDecimal returnRate = position.calculateReturnRate(currentPrice);
        return new PositionDto(
                position.getSymbol(),
                position.getSectorCode(),
                position.getAvgBuyPrice(),
                currentPrice,
                position.getQuantity(),
                marketValue,
                profitLoss,
                returnRate
        );
    }

    public PortfolioSummaryDto toSummaryDto(BigDecimal totalBuyAmount, BigDecimal totalMarketValue, int positionCount) {
        BigDecimal safeBuyAmount = scale4(totalBuyAmount);
        BigDecimal safeMarketValue = scale4(totalMarketValue);
        BigDecimal profitLoss = scale4(safeMarketValue.subtract(safeBuyAmount));
        BigDecimal totalReturnRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (safeBuyAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnRate = profitLoss
                    .divide(safeBuyAmount, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new PortfolioSummaryDto(safeBuyAmount, safeMarketValue, profitLoss, totalReturnRate, positionCount);
    }

    public SectorAllocationDto toSectorAllocationDto(String sectorCode, BigDecimal marketValue, BigDecimal totalMarketValue) {
        BigDecimal safeMarketValue = scale4(marketValue);
        BigDecimal allocationRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            allocationRate = safeMarketValue
                    .divide(totalMarketValue, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new SectorAllocationDto(sectorCode, resolveSectorName(sectorCode), safeMarketValue, allocationRate);
    }

    public WatchlistDto toWatchlistDto(WatchlistItem item, LatestQuote latestQuote) {
        return new WatchlistDto(
                item.getSymbol(),
                item.getSectorCode(),
                latestQuote.getLastPrice().setScale(4, RoundingMode.HALF_UP),
                latestQuote.getChangeRate().setScale(2, RoundingMode.HALF_UP)
        );
    }

    public TradeHistoryDto toTradeHistoryDto(TradeHistory tradeHistory) {
        return new TradeHistoryDto(
                tradeHistory.getSymbol(),
                tradeHistory.getSectorCode(),
                tradeHistory.getTradeType().name(),
                tradeHistory.getPrice().setScale(4, RoundingMode.HALF_UP),
                tradeHistory.getQuantity(),
                tradeHistory.getTradedAt()
        );
    }

    private String resolveSectorName(String sectorCode) {
        return SECTOR_NAMES.getOrDefault(sectorCode, sectorCode);
    }

    private BigDecimal scale4(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
