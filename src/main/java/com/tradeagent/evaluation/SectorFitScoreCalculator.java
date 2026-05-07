package com.tradeagent.evaluation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SectorFitScoreCalculator extends AbstractScoreCalculator<SectorFitScoreInput> {

    @Override
    public BigDecimal calculate(SectorFitScoreInput input) {
        if (input == null || input.sectorScore() == null) {
            return neutralScore();
        }
        return clamp(input.sectorScore());
    }
}
