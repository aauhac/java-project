package com.tradeagent.demo;

import com.tradeagent.portfolio.WatchlistItem;
import com.tradeagent.portfolio.WatchlistRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DemoWatchlistFactory {

    private final WatchlistRepository watchlistRepository;

    public DemoWatchlistFactory(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
    }

    @Transactional
    public void seed(Long userId) {
        List<WatchlistSeed> seeds = List.of(
                new WatchlistSeed("AMD", "SEMI"),
                new WatchlistSeed("AVGO", "SEMI"),
                new WatchlistSeed("RIVN", "EV"),
                new WatchlistSeed("GILD", "BIO"),
                new WatchlistSeed("AMZN", "AIINF")
        );

        for (WatchlistSeed seed : seeds) {
            if (!watchlistRepository.existsByUserIdAndSymbol(userId, seed.symbol())) {
                watchlistRepository.save(new WatchlistItem(userId, seed.symbol(), seed.sectorCode()));
            }
        }
    }

    private record WatchlistSeed(String symbol, String sectorCode) {
    }
}
