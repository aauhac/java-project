package com.tradeagent.common;

public enum ScoreGrade {

    S(90, 100, "최상"),
    A(75, 89, "우수"),
    B(60, 74, "양호"),
    C(40, 59, "보통"),
    D(0, 39, "미흡");

    private final int minScore;
    private final int maxScore;
    private final String label;

    ScoreGrade(int minScore, int maxScore, String label) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.label = label;
    }

    public static ScoreGrade from(double score) {
        int rounded = (int) Math.round(score);
        for (ScoreGrade grade : values()) {
            if (rounded >= grade.minScore && rounded <= grade.maxScore) {
                return grade;
            }
        }
        return D;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getLabel() {
        return label;
    }
}
