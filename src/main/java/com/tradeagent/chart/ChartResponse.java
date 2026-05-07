package com.tradeagent.chart;

import java.time.LocalDate;
import java.util.List;

public record ChartResponse(
        String symbol,
        String timeframe,
        LocalDate start,
        LocalDate end,
        List<ChartBar> bars
) {
}
