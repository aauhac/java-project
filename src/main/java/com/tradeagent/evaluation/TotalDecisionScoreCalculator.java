package com.tradeagent.evaluation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TotalDecisionScoreCalculator {

    public BigDecimal calculate(BigDecimal entryScore,
                                BigDecimal exitScore,
                                BigDecimal riskScore,
                                BigDecimal diversificationScore,
                                BigDecimal sectorFitScore) {
        BigDecimal total = safe(entryScore).multiply(BigDecimal.valueOf(0.25))
                .add(safe(exitScore).multiply(BigDecimal.valueOf(0.25)))
                .add(safe(riskScore).multiply(BigDecimal.valueOf(0.20)))
                .add(safe(diversificationScore).multiply(BigDecimal.valueOf(0.15)))
                .add(safe(sectorFitScore).multiply(BigDecimal.valueOf(0.15)));

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal score) {
        if (score == null) {
            return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }
}
