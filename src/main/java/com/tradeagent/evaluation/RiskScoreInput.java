package com.tradeagent.evaluation;

import com.tradeagent.market.PriceBar;
import com.tradeagent.portfolio.TradeHistory;

import java.util.List;

public record RiskScoreInput(
        TradeHistory buyTrade,
        TradeHistory sellTrade,
        List<PriceBar> holdingBars
) {
}
