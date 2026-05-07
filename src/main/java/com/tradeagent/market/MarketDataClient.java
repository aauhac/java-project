package com.tradeagent.market;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataClient {

    LatestQuote fetchLatestQuote(String symbol);

    List<PriceBar> fetchHistoricalBars(String symbol, LocalDate from, LocalDate to);
}
