package com.tradeagent.sector;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NewsEventDto(
        String sectorCode,
        String symbol,
        String title,
        String source,
        String url,
        BigDecimal toneScore,
        LocalDateTime publishedAt
) {
}
