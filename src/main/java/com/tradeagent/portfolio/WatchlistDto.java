package com.tradeagent.portfolio;

import java.math.BigDecimal;

public record WatchlistDto(
        String symbol,
        String sectorCode,
        BigDecimal currentPrice,
        BigDecimal changeRate
) {
}
