package com.tradeagent.evaluation;

import com.tradeagent.portfolio.PortfolioPosition;

import java.util.List;

public record DiversificationScoreInput(
        Long userId,
        List<PortfolioPosition> positions
) {
}
