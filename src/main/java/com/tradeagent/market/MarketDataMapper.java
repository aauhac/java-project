package com.tradeagent.market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class MarketDataMapper {

    public List<PriceBar> toPriceBars(String symbol, AlpacaBarsResponse response) {
        if (response == null || response.bars() == null) {
            return List.of();
        }
        return response.bars().stream()
                .map(payload -> toPriceBar(symbol, payload))
                .toList();
    }

    public PriceBar toPriceBar(String symbol, AlpacaBarPayload payload) {
        LocalDateTime barTime = LocalDateTime.ofInstant(payload.t(), ZoneOffset.UTC);
        return new PriceBar(
                symbol,
                barTime,
                defaultValue(payload.o()),
                defaultValue(payload.h()),
                defaultValue(payload.l()),
                defaultValue(payload.c()),
                payload.v() == null ? 0L : payload.v()
        );
    }

    public LatestQuote toLatestQuote(String symbol, AlpacaSnapshotResponse response) {
        BigDecimal lastPrice = resolveLastPrice(response);
        BigDecimal previousClose = response != null && response.prevDailyBar() != null
                ? response.prevDailyBar().c()
                : null;
        return new LatestQuote(symbol, lastPrice, calculateChangeRate(lastPrice, previousClose));
    }

    private BigDecimal resolveLastPrice(AlpacaSnapshotResponse response) {
        if (response == null) {
            return BigDecimal.ZERO;
        }
        if (response.latestTrade() != null && response.latestTrade().p() != null) {
            return response.latestTrade().p();
        }
        if (response.dailyBar() != null && response.dailyBar().c() != null) {
            return response.dailyBar().c();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateChangeRate(BigDecimal lastPrice, BigDecimal previousClose) {
        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return lastPrice.subtract(previousClose)
                .divide(previousClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

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

@JsonIgnoreProperties(ignoreUnknown = true)
record AlpacaSnapshotResponse(
        AlpacaTradePayload latestTrade,
        AlpacaDailyBarPayload dailyBar,
        AlpacaDailyBarPayload prevDailyBar
) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AlpacaTradePayload(BigDecimal p) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record AlpacaDailyBarPayload(BigDecimal c) {
}
