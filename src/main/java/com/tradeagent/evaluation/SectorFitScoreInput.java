package com.tradeagent.evaluation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SectorFitScoreInput(
        String symbol,
        String sectorCode,
        LocalDate tradeDate,
        BigDecimal sectorScore
) {
}
