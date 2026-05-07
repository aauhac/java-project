package com.tradeagent.portfolio;

import java.math.BigDecimal;

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
