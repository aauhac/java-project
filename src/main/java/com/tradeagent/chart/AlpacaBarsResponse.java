package com.tradeagent.chart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AlpacaBarsResponse(List<AlpacaBarPayload> bars) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AlpacaBarPayload(
        Instant t,
        BigDecimal o,
        BigDecimal h,
        BigDecimal l,
        BigDecimal c,
        Long v
) {
}
