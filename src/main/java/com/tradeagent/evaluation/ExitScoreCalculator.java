package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.ExitScoreInput;
import com.tradeagent.market.PriceBar;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class ExitScoreCalculator extends AbstractScoreCalculator<ExitScoreInput> {

    @Override
    public double calculate(ExitScoreInput input) {
        double minPrice = input.barsAroundExit().stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .map(java.math.BigDecimal::doubleValue)
                .orElse(Double.NaN);
        double maxPrice = input.barsAroundExit().stream()
                .map(PriceBar::getHighPrice)
                .max(Comparator.naturalOrder())
                .map(java.math.BigDecimal::doubleValue)
                .orElse(Double.NaN);

        double exitPrice = input.tradeHistory().getPrice() == null ? Double.NaN : input.tradeHistory().getPrice().doubleValue();
        return normalize(exitPrice, minPrice, maxPrice);
    }
}
