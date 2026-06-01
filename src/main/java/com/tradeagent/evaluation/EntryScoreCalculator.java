package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.EntryScoreInput;
import com.tradeagent.market.PriceBar;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class EntryScoreCalculator extends AbstractScoreCalculator<EntryScoreInput> {

    @Override
    public double calculate(EntryScoreInput input) {
        double minPrice = input.barsAfterEntry().stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .map(java.math.BigDecimal::doubleValue)
                .orElse(Double.NaN);
        double maxPrice = input.barsAfterEntry().stream()
                .map(PriceBar::getHighPrice)
                .max(Comparator.naturalOrder())
                .map(java.math.BigDecimal::doubleValue)
                .orElse(Double.NaN);

        double entryPrice = input.tradeHistory().getPrice() == null ? Double.NaN : input.tradeHistory().getPrice().doubleValue();
        double position = normalize(entryPrice, minPrice, maxPrice);
        return clamp(100.0 - position);
    }
}
