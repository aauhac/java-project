package com.tradeagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class SectorScoreSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SectorScoreSchemaInitializer.class);

    private final DataSource dataSource;

    public SectorScoreSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasSectorScoreTable(connection)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE sector_score ADD COLUMN IF NOT EXISTS article_count INT DEFAULT 0 NOT NULL");
                statement.execute("ALTER TABLE sector_score ADD COLUMN IF NOT EXISTS avg_tone_score DECIMAL(10,4) DEFAULT 0 NOT NULL");
                statement.execute("ALTER TABLE sector_score ADD COLUMN IF NOT EXISTS keyword_strength_score DECIMAL(6,2) DEFAULT 0 NOT NULL");
                statement.execute("ALTER TABLE sector_score ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'NEUTRAL' NOT NULL");
                statement.execute("ALTER TABLE sector_score ADD COLUMN IF NOT EXISTS analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL");
            }

            log.info("sector_score schema checked");
        }
    }

    private boolean hasSectorScoreTable(Connection connection) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "SECTOR_SCORE", null)) {
            return tables.next();
        }
    }
}
