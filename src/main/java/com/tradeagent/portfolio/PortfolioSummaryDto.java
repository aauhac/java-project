package com.tradeagent.portfolio;

import java.math.BigDecimal;

public record PortfolioSummaryDto(
        BigDecimal totalBuyAmount,
        BigDecimal totalMarketValue,
        BigDecimal totalProfitLoss,
        BigDecimal totalReturnRate,
        Integer positionCount
) {
}
