package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.RiskScoreInput;
import com.tradeagent.market.PriceBar;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class RiskScoreCalculator extends AbstractScoreCalculator<RiskScoreInput> {

    @Override
    public double calculate(RiskScoreInput input) {
        if (input == null || input.buyTrade() == null || input.buyTrade().getPrice() == null) {
            return neutralScore();
        }
        double buyPrice = input.buyTrade().getPrice().doubleValue();
        if (!Double.isFinite(buyPrice) || buyPrice <= 0) {
            return neutralScore();
        }

        double minLow = input.holdingBars().stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .map(java.math.BigDecimal::doubleValue)
                .orElse(buyPrice);

        double drawdown = Math.max(0.0, buyPrice - minLow);
        double drawdownPercent = (drawdown / buyPrice) * 100.0;
        double penalty = normalize(drawdownPercent, 0.0, 15.0);
        return clamp(100.0 - penalty);
    }
}
