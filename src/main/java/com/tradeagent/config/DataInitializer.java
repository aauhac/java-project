package com.tradeagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SeedProperties seedProperties;
    private final ReferenceDataSeeder referenceDataSeeder;

    public DataInitializer(SeedProperties seedProperties,
                          ReferenceDataSeeder referenceDataSeeder) {
        this.seedProperties = seedProperties;
        this.referenceDataSeeder = referenceDataSeeder;
    }

    @Override
    public void run(String... args) {
        if (!seedProperties.isEnabled()) {
            log.info("Data initialization disabled (trade.seed.enabled=false)");
            return;
        }

        if (seedProperties.isReferenceDataEnabled()) {
            log.info("Seeding reference data (sectors, symbols, proxies)...");
            referenceDataSeeder.seed();
            log.info("Reference data seed completed");
        }

        if (seedProperties.isSampleDataEnabled()) {
            log.info("Sample data seeding enabled but not implemented in this phase");
        }

        log.info("Data initialization completed");
    }
}
