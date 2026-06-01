package com.tradeagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ReferenceDataSeeder referenceDataSeeder;

    public DataInitializer(ReferenceDataSeeder referenceDataSeeder) {
        this.referenceDataSeeder = referenceDataSeeder;
    }

    @Override
    public void run(String... args) {
        log.info("Seeding reference data (sectors, symbols, proxies)...");
        referenceDataSeeder.seed();
        log.info("Reference data seed completed");
        log.info("Data initialization completed");
    }
}
