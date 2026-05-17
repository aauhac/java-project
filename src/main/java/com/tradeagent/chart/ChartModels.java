package com.tradeagent.chart;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ChartModels {

    private ChartModels() {
    }

    public record ChartBar(
            Instant time,
            double open,
            double high,
            double low,
            double close,
            long volume
    ) {
    }

    public record ChartResponse(
            String symbol,
            String timeframe,
            LocalDate start,
            LocalDate end,
            List<ChartBar> bars
    ) {
    }
}
