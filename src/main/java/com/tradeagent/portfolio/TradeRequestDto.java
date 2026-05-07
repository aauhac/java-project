package com.tradeagent.portfolio;

import java.math.BigDecimal;

public record TradeRequestDto(
        Long userId,
        String symbol,
        String sectorCode,
        BigDecimal price,
        Integer quantity
) {
}
