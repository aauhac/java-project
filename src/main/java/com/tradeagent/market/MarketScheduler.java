package com.tradeagent.market;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketScheduler {

    private static final List<String> WATCHLIST = List.of("AAPL", "NVDA", "TSLA");

    private final MarketDataService marketDataService;

    public MarketScheduler(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Scheduled(fixedDelay = 60000)
    public void refreshWatchlistQuotes() {
        WATCHLIST.forEach(marketDataService::refreshLatestQuote);
    }
}
