package com.tradeagent.evaluation;

import com.tradeagent.market.PriceBar;
import com.tradeagent.portfolio.TradeHistory;

import java.util.List;

public record ExitScoreInput(
        TradeHistory tradeHistory,
        List<PriceBar> barsAroundExit
) {
}
