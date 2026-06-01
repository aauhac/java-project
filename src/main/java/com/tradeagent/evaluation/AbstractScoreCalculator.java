package com.tradeagent.evaluation;

public abstract class AbstractScoreCalculator<T> implements ScoreCalculator<T> {

    protected double clamp(double score) {
        if (!Double.isFinite(score)) return neutralScore();
        if (score < 0) return 0.0;
        if (score > 100) return 100.0;
        return Math.round(score * 100.0) / 100.0;
    }

    protected double normalize(double value, double min, double max) {
        if (!Double.isFinite(value) || !Double.isFinite(min) || !Double.isFinite(max)) {
            return neutralScore();
        }
        if (max == min) return neutralScore();

        return clamp((value - min) / (max - min) * 100.0);
    }

    protected double neutralScore() {
        return 50.0;
    }
}