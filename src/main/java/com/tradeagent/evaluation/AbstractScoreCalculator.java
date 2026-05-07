package com.tradeagent.evaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class AbstractScoreCalculator<T> implements ScoreCalculator<T> {

    protected BigDecimal clamp(BigDecimal score) {
        if (score == null) {
            return neutralScore();
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal normalize(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null || min == null || max == null) {
            return neutralScore();
        }
        if (max.compareTo(min) == 0) {
            return neutralScore();
        }

        BigDecimal normalized = value.subtract(min)
                .divide(max.subtract(min), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return clamp(normalized);
    }

    protected BigDecimal neutralScore() {
        return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
    }
}
