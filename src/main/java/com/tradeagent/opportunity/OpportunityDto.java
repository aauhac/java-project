package com.tradeagent.opportunity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OpportunityDto(
        String type,
        String symbol,
        String sectorCode,
        BigDecimal changeRate,
        String reason,
        String feedback,
        LocalDateTime detectedAt
) {
}
