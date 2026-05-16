package com.tradeagent.config;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.sector.SectorMaster;
import com.tradeagent.sector.SectorMasterRepository;
import com.tradeagent.sector.SectorProxy;
import com.tradeagent.sector.SectorProxyRepository;
import com.tradeagent.sector.SectorScore;
import com.tradeagent.sector.SectorScoreRepository;
import com.tradeagent.sector.SymbolSectorMap;
import com.tradeagent.sector.SymbolSectorMapRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class ReferenceDataSeeder {

    private final SectorMasterRepository sectorMasterRepository;
    private final SymbolSectorMapRepository symbolSectorMapRepository;
    private final SectorProxyRepository sectorProxyRepository;
    private final SectorScoreRepository sectorScoreRepository;

    public ReferenceDataSeeder(SectorMasterRepository sectorMasterRepository,
                               SymbolSectorMapRepository symbolSectorMapRepository,
                               SectorProxyRepository sectorProxyRepository,
                               SectorScoreRepository sectorScoreRepository) {
        this.sectorMasterRepository = sectorMasterRepository;
        this.symbolSectorMapRepository = symbolSectorMapRepository;
        this.sectorProxyRepository = sectorProxyRepository;
        this.sectorScoreRepository = sectorScoreRepository;
    }

    @Transactional
    public void seed() {
        seedMasters();
        seedMappings();
        seedProxies();
        seedScores();
    }

    private void seedMasters() {
        List<SectorMaster> masters = List.of(
                new SectorMaster("SEMI", "반도체", "Semiconductor designers, foundries, and equipment leaders."),
                new SectorMaster("AIINF", "AI 인프라", "AI infrastructure, hyperscalers, and compute platforms."),
                new SectorMaster("EV", "전기차", "Electric vehicle manufacturers and battery ecosystem."),
                new SectorMaster("BIO", "바이오", "Biotech and biopharma innovators.")
        );

        for (SectorMaster master : masters) {
            sectorMasterRepository.findBySectorCode(master.getSectorCode())
                    .orElseGet(() -> sectorMasterRepository.save(master));
        }
    }

    private void seedMappings() {
        List<SymbolSectorMap> mappings = List.of(
                new SymbolSectorMap("NVDA", "SEMI", true, BigDecimal.valueOf(1.00)),
                new SymbolSectorMap("AMD", "SEMI", false, BigDecimal.valueOf(0.80)),
                new SymbolSectorMap("AVGO", "SEMI", false, BigDecimal.valueOf(0.85)),
                new SymbolSectorMap("TSM", "SEMI", false, BigDecimal.valueOf(0.90)),
                new SymbolSectorMap("NVDA", "AIINF", false, BigDecimal.valueOf(0.85)),
                new SymbolSectorMap("MSFT", "AIINF", true, BigDecimal.valueOf(1.00)),
                new SymbolSectorMap("AMZN", "AIINF", false, BigDecimal.valueOf(0.90)),
                new SymbolSectorMap("GOOGL", "AIINF", false, BigDecimal.valueOf(0.80)),
                new SymbolSectorMap("TSLA", "EV", true, BigDecimal.valueOf(1.00)),
                new SymbolSectorMap("RIVN", "EV", false, BigDecimal.valueOf(0.60)),
                new SymbolSectorMap("LI", "EV", false, BigDecimal.valueOf(0.70)),
                new SymbolSectorMap("MRNA", "BIO", true, BigDecimal.valueOf(0.80)),
                new SymbolSectorMap("GILD", "BIO", false, BigDecimal.valueOf(0.90)),
                new SymbolSectorMap("AMGN", "BIO", false, BigDecimal.valueOf(1.00))
        );

        for (SymbolSectorMap mapping : mappings) {
            symbolSectorMapRepository.findBySymbolAndSectorCode(mapping.getSymbol(), mapping.getSectorCode())
                    .orElseGet(() -> symbolSectorMapRepository.save(mapping));
        }
    }

    private void seedProxies() {
        List<SectorProxy> proxies = List.of(
                new SectorProxy("SEMI", "NVDA", "STOCK", BigDecimal.valueOf(1.00)),
                new SectorProxy("SEMI", "AMD", "STOCK", BigDecimal.valueOf(0.80)),
                new SectorProxy("SEMI", "TSM", "STOCK", BigDecimal.valueOf(0.90)),
                new SectorProxy("SEMI", "SMH", "ETF", BigDecimal.valueOf(0.75)),
                new SectorProxy("AIINF", "MSFT", "STOCK", BigDecimal.valueOf(1.00)),
                new SectorProxy("AIINF", "NVDA", "STOCK", BigDecimal.valueOf(0.90)),
                new SectorProxy("AIINF", "AMZN", "STOCK", BigDecimal.valueOf(0.85)),
                new SectorProxy("AIINF", "GOOGL", "STOCK", BigDecimal.valueOf(0.80)),
                new SectorProxy("EV", "TSLA", "STOCK", BigDecimal.valueOf(1.00)),
                new SectorProxy("EV", "RIVN", "STOCK", BigDecimal.valueOf(0.60)),
                new SectorProxy("EV", "LI", "STOCK", BigDecimal.valueOf(0.70)),
                new SectorProxy("BIO", "MRNA", "STOCK", BigDecimal.valueOf(0.80)),
                new SectorProxy("BIO", "GILD", "STOCK", BigDecimal.valueOf(0.90)),
                new SectorProxy("BIO", "AMGN", "STOCK", BigDecimal.valueOf(1.00)),
                new SectorProxy("BIO", "IBB", "ETF", BigDecimal.valueOf(0.70))
        );

        for (SectorProxy proxy : proxies) {
            sectorProxyRepository.findBySectorCodeAndProxySymbol(proxy.getSectorCode(), proxy.getProxySymbol())
                    .orElseGet(() -> sectorProxyRepository.save(proxy));
        }
    }

    private void seedScores() {
        LocalDate today = DateTimeUtil.today();
        seedScore(today, "SEMI", "84.00", "82.00", "91.00", "86.00", "92.00", "87.00");
        seedScore(today, "AIINF", "80.00", "78.00", "85.00", "79.00", "84.00", "81.00");
        seedScore(today, "EV", "45.00", "40.00", "38.00", "44.00", "43.00", "42.00");
        seedScore(today, "BIO", "56.00", "54.00", "57.00", "53.00", "55.00", "55.00");
    }

    private void seedScore(LocalDate date, String sectorCode, String newsVolume, String newsTone,
                           String momentum, String volumeSpike, String breadth, String total) {
        sectorScoreRepository.findBySectorCodeAndScoreDate(sectorCode, date)
                .orElseGet(() -> sectorScoreRepository.save(new SectorScore(
                        sectorCode,
                        date,
                        new BigDecimal(newsVolume),
                        new BigDecimal(newsTone),
                        new BigDecimal(momentum),
                        new BigDecimal(volumeSpike),
                        new BigDecimal(breadth),
                        new BigDecimal(total)
                )));
    }
}
