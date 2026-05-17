package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.ExitScoreInput;
import com.tradeagent.market.PriceBar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
public class ExitScoreCalculator extends AbstractScoreCalculator<ExitScoreInput> {

    @Override
    public BigDecimal calculate(ExitScoreInput input) {
        BigDecimal minPrice = input.barsAroundExit().stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);
        BigDecimal maxPrice = input.barsAroundExit().stream()
                .map(PriceBar::getHighPrice)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return normalize(input.tradeHistory().getPrice(), minPrice, maxPrice);
    }
}
