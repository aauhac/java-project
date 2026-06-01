package com.tradeagent.evaluation;

public interface ScoreCalculator<T> {

    double calculate(T input);
}
