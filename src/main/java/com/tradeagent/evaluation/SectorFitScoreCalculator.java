package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.SectorFitScoreInput;
import org.springframework.stereotype.Component;

@Component
public class SectorFitScoreCalculator extends AbstractScoreCalculator<SectorFitScoreInput> {

    @Override
    public double calculate(SectorFitScoreInput input) {
        if (input == null || input.sectorScore() == null) {
            return neutralScore();
        }
        return clamp(input.sectorScore().doubleValue());
    }
}
