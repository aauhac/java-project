package com.tradeagent.evaluation;

import java.math.BigDecimal;

public record ScoreDetailDto(
        String name,
        BigDecimal score,
        String comment
) {
}
