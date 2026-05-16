package com.tradeagent.demo;

import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.PortfolioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Disabled: using seed-based DataInitializer instead
// @Component
public class DemoPortfolioFactory {

    private final PortfolioRepository portfolioRepository;

    public DemoPortfolioFactory(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Transactional
    public void seed(Long userId) {
        List<PortfolioSeed> seeds = List.of(
                new PortfolioSeed("NVDA", "SEMI", "780.0000", 2),
                new PortfolioSeed("TSLA", "EV", "225.0000", 3),
                new PortfolioSeed("MSFT", "AIINF", "405.0000", 1),
                new PortfolioSeed("MRNA", "BIO", "110.0000", 4)
        );

        for (PortfolioSeed seed : seeds) {
            portfolioRepository.findByUserIdAndSymbol(userId, seed.symbol())
                    .orElseGet(() -> portfolioRepository.save(new PortfolioPosition(
                            userId,
                            seed.symbol(),
                            seed.sectorCode(),
                            seed.avgBuyPrice(),
                            seed.quantity(),
                            seed.avgBuyPrice().multiply(BigDecimal.valueOf(seed.quantity())).setScale(4, RoundingMode.HALF_UP)
                    )));
        }
    }

    private record PortfolioSeed(String symbol, String sectorCode, String avgBuyPriceText, int quantity) {

        private BigDecimal avgBuyPrice() {
            return new BigDecimal(avgBuyPriceText).setScale(4, RoundingMode.HALF_UP);
        }
    }
}
