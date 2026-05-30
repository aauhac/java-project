package com.tradeagent.config;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.sector.SectorMaster;
import com.tradeagent.sector.SectorMasterRepository;
import com.tradeagent.sector.SectorProxy;
import com.tradeagent.sector.SectorProxyRepository;
import com.tradeagent.sector.SymbolSectorMap;
import com.tradeagent.sector.SymbolSectorMapRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReferenceDataSeeder {

    private final SectorMasterRepository sectorMasterRepository;
    private final SymbolSectorMapRepository symbolSectorMapRepository;
    private final SectorProxyRepository sectorProxyRepository;

    public ReferenceDataSeeder(SectorMasterRepository sectorMasterRepository,
                               SymbolSectorMapRepository symbolSectorMapRepository,
                               SectorProxyRepository sectorProxyRepository) {
        this.sectorMasterRepository = sectorMasterRepository;
        this.symbolSectorMapRepository = symbolSectorMapRepository;
        this.sectorProxyRepository = sectorProxyRepository;
    }

    @Transactional
    public void seed() {
        seedMasters();
        seedMappings();
        seedProxies();
    }

    private void seedMasters() {
        List<SectorMaster> masters = List.of(
                new SectorMaster("SEMI", "반도체", "Semiconductor designers, foundries, and equipment leaders."),
                new SectorMaster("AIINF", "AI 인프라", "AI infrastructure, hyperscalers, and compute platforms."),
                new SectorMaster("EV", "전기차", "Electric vehicle manufacturers and battery ecosystem."),
                new SectorMaster("BIO", "바이오", "Biotech and biopharma innovators."),
                new SectorMaster("CLOUD", "클라우드", "Cloud software, SaaS platforms, and enterprise infrastructure."),
                new SectorMaster("ENERGY", "에너지", "Energy, oil, gas, utilities, and raw materials ecosystem.")
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
                new SymbolSectorMap("AMGN", "BIO", false, BigDecimal.valueOf(1.00)),
                new SymbolSectorMap("MSFT", "CLOUD", true, BigDecimal.valueOf(1.00)),
                new SymbolSectorMap("ORCL", "CLOUD", false, BigDecimal.valueOf(0.85)),
                new SymbolSectorMap("CRM", "CLOUD", false, BigDecimal.valueOf(0.80)),
                new SymbolSectorMap("NOW", "CLOUD", false, BigDecimal.valueOf(0.75)),
                new SymbolSectorMap("XOM", "ENERGY", true, BigDecimal.valueOf(1.00)),
                new SymbolSectorMap("CVX", "ENERGY", false, BigDecimal.valueOf(0.95)),
                new SymbolSectorMap("SLB", "ENERGY", false, BigDecimal.valueOf(0.80)),
                new SymbolSectorMap("CCJ", "ENERGY", false, BigDecimal.valueOf(0.70))
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
                new SectorProxy("BIO", "IBB", "ETF", BigDecimal.valueOf(0.70)),
                new SectorProxy("CLOUD", "MSFT", "STOCK", BigDecimal.valueOf(1.00)),
                new SectorProxy("CLOUD", "ORCL", "STOCK", BigDecimal.valueOf(0.85)),
                new SectorProxy("CLOUD", "CRM", "STOCK", BigDecimal.valueOf(0.80)),
                new SectorProxy("CLOUD", "NOW", "STOCK", BigDecimal.valueOf(0.75)),
                new SectorProxy("ENERGY", "XOM", "STOCK", BigDecimal.valueOf(1.00)),
                new SectorProxy("ENERGY", "CVX", "STOCK", BigDecimal.valueOf(0.95)),
                new SectorProxy("ENERGY", "SLB", "STOCK", BigDecimal.valueOf(0.80)),
                new SectorProxy("ENERGY", "XLE", "ETF", BigDecimal.valueOf(0.75))
        );

        for (SectorProxy proxy : proxies) {
            sectorProxyRepository.findBySectorCodeAndProxySymbol(proxy.getSectorCode(), proxy.getProxySymbol())
                    .orElseGet(() -> sectorProxyRepository.save(proxy));
        }
    }
}
