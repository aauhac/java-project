package com.tradeagent.market;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MarketScheduler.class);
    private static final List<String> WATCHLIST = List.of("AAPL", "NVDA", "TSLA");

    private final MarketDataService marketDataService;

    public MarketScheduler(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Scheduled(fixedDelay = 60000)
    public void refreshWatchlistQuotes() {
        WATCHLIST.forEach(symbol -> {
            try {
                marketDataService.refreshLatestQuote(symbol);
            } catch (Exception e) {
                // 슬립 재개 후 stale connection 에러 포함 — 조용히 무시하고 다음 주기에 재시도
                logger.trace("Quote refresh skipped for {} – {}", symbol, e.getClass().getSimpleName());
            }
        });
    }
}
