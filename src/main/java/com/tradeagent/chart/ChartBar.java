package com.tradeagent.chart;

import java.time.Instant;

public record ChartBar(
        Instant time,
        double open,
        double high,
        double low,
        double close,
        long volume
) {
}
