package com.tradeagent.evaluation;

import org.springframework.stereotype.Component;

@Component
public class TotalDecisionScoreCalculator {

    public double calculate(double entryScore,
                            double exitScore,
                            double riskScore,
                            double diversificationScore,
                            double sectorFitScore) {
        double total = safe(entryScore) * 0.25
                + safe(exitScore) * 0.25
                + safe(riskScore) * 0.20
                + safe(diversificationScore) * 0.15
                + safe(sectorFitScore) * 0.15;
        return clamp(total);
    }

    private double safe(double score) {
        if (!Double.isFinite(score)) {
            return 50.0;
        }
        return clamp(score);
    }

    private double clamp(double score) {
        if (score < 0) {
            return 0.0;
        }
        if (score > 100) {
            return 100.0;
        }
        return Math.round(score * 100.0) / 100.0;
    }
}
