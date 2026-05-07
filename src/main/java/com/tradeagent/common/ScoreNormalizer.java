package com.tradeagent.common;

public final class ScoreNormalizer {

    private ScoreNormalizer() {}

    /**
     * 0~100 범위로 클램핑
     */
    public static double clamp(double score) {
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * 가중 평균 계산 (scores와 weights 길이가 같아야 함)
     */
    public static double weightedAverage(double[] scores, double[] weights) {
        if (scores.length != weights.length || scores.length == 0) {
            throw new IllegalArgumentException("scores and weights must have the same non-zero length");
        }
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < scores.length; i++) {
            weightedSum += clamp(scores[i]) * weights[i];
            totalWeight += weights[i];
        }
        if (totalWeight == 0.0) return 0.0;
        return clamp(weightedSum / totalWeight);
    }

    /**
     * minVal~maxVal 범위의 값을 0~100으로 선형 정규화
     */
    public static double normalize(double value, double minVal, double maxVal) {
        if (maxVal <= minVal) return 0.0;
        return clamp((value - minVal) / (maxVal - minVal) * 100.0);
    }

    /**
     * 소수점 첫째 자리 반올림
     */
    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
