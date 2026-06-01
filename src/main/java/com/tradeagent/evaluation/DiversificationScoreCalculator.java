package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.DiversificationScoreInput;
import com.tradeagent.portfolio.PortfolioPosition;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class DiversificationScoreCalculator extends AbstractScoreCalculator<DiversificationScoreInput> {

    @Override
    public double calculate(DiversificationScoreInput input) {
        double totalAmount = input.positions().stream()
                .map(PortfolioPosition::getTotalBuyAmount)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();

        if (totalAmount <= 0) {
            return neutralScore();
        }

        double maxWeight = input.positions().stream()
                .map(position -> (position.getTotalBuyAmount().doubleValue() / totalAmount) * 100.0)
                .max(Comparator.naturalOrder())
                .orElse(100.0);

        double concentrationScore = clamp(100.0 - normalize(maxWeight, 20.0, 80.0));
        double positionCountScore = normalize(input.positions().size(), 1.0, 8.0);
        double weighted = concentrationScore * 0.7 + positionCountScore * 0.3;
        return clamp(weighted);
    }
}
