package com.tradeagent.evaluation;

import com.tradeagent.market.PriceBar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

@Component
public class RiskScoreCalculator extends AbstractScoreCalculator<RiskScoreInput> {

    @Override
    public BigDecimal calculate(RiskScoreInput input) {
        BigDecimal buyPrice = input.buyTrade().getPrice();
        if (buyPrice == null || buyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return neutralScore();
        }

        BigDecimal minLow = input.holdingBars().stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(buyPrice);

        BigDecimal drawdown = buyPrice.subtract(minLow);
        if (drawdown.compareTo(BigDecimal.ZERO) < 0) {
            drawdown = BigDecimal.ZERO;
        }

        BigDecimal drawdownPercent = drawdown
                .divide(buyPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        BigDecimal penalty = normalize(drawdownPercent, BigDecimal.ZERO, BigDecimal.valueOf(15));
        return clamp(BigDecimal.valueOf(100).subtract(penalty));
    }
}
