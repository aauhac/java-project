package com.tradeagent.evaluation;

import com.tradeagent.market.PriceBar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
public class EntryScoreCalculator extends AbstractScoreCalculator<EntryScoreInput> {

    @Override
    public BigDecimal calculate(EntryScoreInput input) {
        if (input == null || input.tradeHistory() == null || input.barsAfterEntry() == null || input.barsAfterEntry().isEmpty()) {
            return neutralScore();
        }

        BigDecimal minPrice = input.barsAfterEntry().stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);
        BigDecimal maxPrice = input.barsAfterEntry().stream()
                .map(PriceBar::getHighPrice)
                .max(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal position = normalize(input.tradeHistory().getPrice(), minPrice, maxPrice);
        return clamp(BigDecimal.valueOf(100).subtract(position));
    }
}
