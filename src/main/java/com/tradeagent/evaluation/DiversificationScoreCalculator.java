package com.tradeagent.evaluation;

import com.tradeagent.portfolio.PortfolioPosition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

@Component
public class DiversificationScoreCalculator extends AbstractScoreCalculator<DiversificationScoreInput> {

    @Override
    public BigDecimal calculate(DiversificationScoreInput input) {
        BigDecimal totalAmount = input.positions().stream()
                .map(PortfolioPosition::getTotalBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return neutralScore();
        }

        BigDecimal maxWeight = input.positions().stream()
                .map(position -> position.getTotalBuyAmount()
                        .divide(totalAmount, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.valueOf(100));

        BigDecimal concentrationScore = clamp(BigDecimal.valueOf(100)
                .subtract(normalize(maxWeight, BigDecimal.valueOf(20), BigDecimal.valueOf(80))));
        BigDecimal positionCountScore = normalize(BigDecimal.valueOf(input.positions().size()), BigDecimal.ONE, BigDecimal.valueOf(8));

        BigDecimal weighted = concentrationScore.multiply(BigDecimal.valueOf(0.7))
                .add(positionCountScore.multiply(BigDecimal.valueOf(0.3)));
        return clamp(weighted);
    }
}
