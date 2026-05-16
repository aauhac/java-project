package com.tradeagent.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Disabled: using seed-based DataInitializer instead
// @Component
public class DemoDataInitializer implements CommandLineRunner {

    private final DemoUserFactory demoUserFactory;
    private final DemoPortfolioFactory demoPortfolioFactory;
    private final DemoTradeFactory demoTradeFactory;
    private final DemoWatchlistFactory demoWatchlistFactory;
    private final DemoSectorFactory demoSectorFactory;
    private final DemoMarketDataFactory demoMarketDataFactory;

    public DemoDataInitializer(DemoUserFactory demoUserFactory,
                               DemoPortfolioFactory demoPortfolioFactory,
                               DemoTradeFactory demoTradeFactory,
                               DemoWatchlistFactory demoWatchlistFactory,
                               DemoSectorFactory demoSectorFactory,
                               DemoMarketDataFactory demoMarketDataFactory) {
        this.demoUserFactory = demoUserFactory;
        this.demoPortfolioFactory = demoPortfolioFactory;
        this.demoTradeFactory = demoTradeFactory;
        this.demoWatchlistFactory = demoWatchlistFactory;
        this.demoSectorFactory = demoSectorFactory;
        this.demoMarketDataFactory = demoMarketDataFactory;
    }

    @Override
    public void run(String... args) {
        Long userId = demoUserFactory.ensureDemoUser().getUserId();
        demoSectorFactory.seed();
        demoMarketDataFactory.seed();
        demoPortfolioFactory.seed(userId);
        demoTradeFactory.seed(userId);
        demoWatchlistFactory.seed(userId);
    }
}
