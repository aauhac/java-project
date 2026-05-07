package com.tradeagent.evaluation;

import java.math.BigDecimal;

public interface ScoreCalculator<T> {

    BigDecimal calculate(T input);
}
