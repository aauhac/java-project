package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.sector.SectorApiModels.NewsEventDto;
import com.tradeagent.sector.SectorApiModels.SectorScoreDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SectorAnalysisService {

    private final SectorMasterRepository sectorMasterRepository;
    private final SymbolSectorMapRepository symbolSectorMapRepository;
    private final SectorProxyRepository sectorProxyRepository;
    private final SectorScoreRepository sectorScoreRepository;
    private final SectorScoreCalculator sectorScoreCalculator;
    private final NewsSignalAggregator newsSignalAggregator;

    public SectorAnalysisService(SectorMasterRepository sectorMasterRepository,
                                 SymbolSectorMapRepository symbolSectorMapRepository,
                                 SectorProxyRepository sectorProxyRepository,
                                 SectorScoreRepository sectorScoreRepository,
                                 SectorScoreCalculator sectorScoreCalculator,
                                 NewsSignalAggregator newsSignalAggregator) {
        this.sectorMasterRepository = sectorMasterRepository;
        this.symbolSectorMapRepository = symbolSectorMapRepository;
        this.sectorProxyRepository = sectorProxyRepository;
        this.sectorScoreRepository = sectorScoreRepository;
        this.sectorScoreCalculator = sectorScoreCalculator;
        this.newsSignalAggregator = newsSignalAggregator;
    }

    @Transactional
    public List<SectorScoreDto> calculateTodaySectorScores() {
        ensureSeedData();
        LocalDate today = DateTimeUtil.today();
        return sectorMasterRepository.findAllByOrderBySectorCodeAsc().stream()
                .map(master -> upsertSectorScore(master, today))
                .sorted(Comparator.comparing(SectorScoreDto::totalSectorScore).reversed())
                .toList();
    }

    @Transactional
    public List<SectorScoreDto> getLatestSectorScores() {
        ensureSeedData();
        return sectorMasterRepository.findAllByOrderBySectorCodeAsc().stream()
                .map(master -> sectorScoreRepository.findTopBySectorCodeOrderByScoreDateDesc(master.getSectorCode())
                        .map(score -> toDto(master, score))
                        .orElseGet(() -> upsertSectorScore(master, DateTimeUtil.today())))
                .sorted(Comparator.comparing(SectorScoreDto::totalSectorScore).reversed())
                .toList();
    }

    @Transactional
    public SectorScoreDto getSectorScore(String sectorCode) {
        ensureSeedData();
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        SectorMaster master = sectorMasterRepository.findBySectorCode(resolvedSectorCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                        "sector not found for code " + resolvedSectorCode));
        return sectorScoreRepository.findTopBySectorCodeOrderByScoreDateDesc(resolvedSectorCode)
                .map(score -> toDto(master, score))
                .orElseGet(() -> upsertSectorScore(master, DateTimeUtil.today()));
    }

    @Transactional
    public List<NewsEventDto> getSectorNews(String sectorCode) {
        ensureSeedData();
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        sectorMasterRepository.findBySectorCode(resolvedSectorCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                        "sector not found for code " + resolvedSectorCode));

        return newsSignalAggregator.getNewsEvents(resolvedSectorCode, DateTimeUtil.today()).stream()
                .sorted(Comparator.comparing(NewsEvent::getPublishedAt).reversed())
                .map(event -> new NewsEventDto(
                        event.getSectorCode(),
                        event.getSymbol(),
                        event.getTitle(),
                        event.getSource(),
                        event.getUrl(),
                        event.getToneScore().setScale(2, RoundingMode.HALF_UP),
                        event.getPublishedAt()
                ))
                .toList();
    }

    private SectorScoreDto upsertSectorScore(SectorMaster master, LocalDate date) {
        SectorScore calculated = sectorScoreCalculator.calculate(master.getSectorCode(), date);
        SectorScore saved = sectorScoreRepository.findBySectorCodeAndScoreDate(master.getSectorCode(), date)
                .map(existing -> {
                    existing.updateScores(
                            calculated.getNewsVolumeScore(),
                            calculated.getNewsToneScore(),
                            calculated.getPriceMomentumScore(),
                            calculated.getVolumeSpikeScore(),
                            calculated.getBreadthScore(),
                            calculated.getTotalSectorScore(),
                            calculated.getStatus(),
                            calculated.getAnalyzedAt()
                    );
                    return sectorScoreRepository.save(existing);
                })
                .orElseGet(() -> sectorScoreRepository.save(calculated));
        return toDto(master, saved);
    }

    private SectorScoreDto toDto(SectorMaster master, SectorScore score) {
        return new SectorScoreDto(
                master.getSectorCode(),
                master.getSectorName(),
                score.getScoreDate(),
                score.getNewsVolumeScore().setScale(2, RoundingMode.HALF_UP),
                score.getNewsToneScore().setScale(2, RoundingMode.HALF_UP),
                score.getPriceMomentumScore().setScale(2, RoundingMode.HALF_UP),
                score.getVolumeSpikeScore().setScale(2, RoundingMode.HALF_UP),
                score.getBreadthScore().setScale(2, RoundingMode.HALF_UP),
                score.getTotalSectorScore().setScale(2, RoundingMode.HALF_UP),
                score.getStatus()
        );
    }

    private void ensureSeedData() {
        seedSectorMasters();
        seedSymbolSectorMappings();
        seedSectorProxies();
    }

    private void seedSectorMasters() {
        saveSectorMasterIfMissing("SEMI", "반도체", "Semiconductor designers, foundries, and equipment leaders.");
        saveSectorMasterIfMissing("AIINF", "AI 인프라", "AI infrastructure, hyperscalers, and compute platforms.");
        saveSectorMasterIfMissing("EV", "전기차", "Electric vehicle manufacturers and battery ecosystem.");
        saveSectorMasterIfMissing("BIO", "바이오", "Biotech and biopharma innovators.");
    }

    private void seedSymbolSectorMappings() {
        saveSymbolSectorMapIfMissing("NVDA", "SEMI", true, BigDecimal.valueOf(1.00));
        saveSymbolSectorMapIfMissing("AMD", "SEMI", false, BigDecimal.valueOf(0.80));
        saveSymbolSectorMapIfMissing("TSM", "SEMI", false, BigDecimal.valueOf(0.90));
        saveSymbolSectorMapIfMissing("NVDA", "AIINF", true, BigDecimal.valueOf(0.90));
        saveSymbolSectorMapIfMissing("MSFT", "AIINF", false, BigDecimal.valueOf(1.00));
        saveSymbolSectorMapIfMissing("AMZN", "AIINF", false, BigDecimal.valueOf(0.80));
        saveSymbolSectorMapIfMissing("GOOGL", "AIINF", false, BigDecimal.valueOf(0.80));
        saveSymbolSectorMapIfMissing("TSLA", "EV", true, BigDecimal.valueOf(1.00));
        saveSymbolSectorMapIfMissing("RIVN", "EV", false, BigDecimal.valueOf(0.60));
        saveSymbolSectorMapIfMissing("LI", "EV", false, BigDecimal.valueOf(0.70));
        saveSymbolSectorMapIfMissing("MRNA", "BIO", true, BigDecimal.valueOf(0.80));
        saveSymbolSectorMapIfMissing("AMGN", "BIO", false, BigDecimal.valueOf(1.00));
        saveSymbolSectorMapIfMissing("GILD", "BIO", false, BigDecimal.valueOf(0.90));
    }

    private void seedSectorProxies() {
        saveSectorProxyIfMissing("SEMI", "NVDA", "STOCK", BigDecimal.valueOf(1.00));
        saveSectorProxyIfMissing("SEMI", "AMD", "STOCK", BigDecimal.valueOf(0.80));
        saveSectorProxyIfMissing("SEMI", "TSM", "STOCK", BigDecimal.valueOf(0.90));
        saveSectorProxyIfMissing("SEMI", "SMH", "ETF", BigDecimal.valueOf(0.70));
        saveSectorProxyIfMissing("AIINF", "NVDA", "STOCK", BigDecimal.valueOf(0.90));
        saveSectorProxyIfMissing("AIINF", "MSFT", "STOCK", BigDecimal.valueOf(1.00));
        saveSectorProxyIfMissing("AIINF", "AMZN", "STOCK", BigDecimal.valueOf(0.80));
        saveSectorProxyIfMissing("AIINF", "GOOGL", "STOCK", BigDecimal.valueOf(0.80));
        saveSectorProxyIfMissing("EV", "TSLA", "STOCK", BigDecimal.valueOf(1.00));
        saveSectorProxyIfMissing("EV", "RIVN", "STOCK", BigDecimal.valueOf(0.60));
        saveSectorProxyIfMissing("EV", "LI", "STOCK", BigDecimal.valueOf(0.70));
        saveSectorProxyIfMissing("BIO", "MRNA", "STOCK", BigDecimal.valueOf(0.80));
        saveSectorProxyIfMissing("BIO", "AMGN", "STOCK", BigDecimal.valueOf(1.00));
        saveSectorProxyIfMissing("BIO", "GILD", "STOCK", BigDecimal.valueOf(0.90));
        saveSectorProxyIfMissing("BIO", "IBB", "ETF", BigDecimal.valueOf(0.70));
    }

    private void saveSectorMasterIfMissing(String sectorCode, String sectorName, String description) {
        sectorMasterRepository.findBySectorCode(sectorCode)
                .orElseGet(() -> sectorMasterRepository.save(new SectorMaster(sectorCode, sectorName, description)));
    }

    private void saveSymbolSectorMapIfMissing(String symbol, String sectorCode, boolean primary, BigDecimal weight) {
        symbolSectorMapRepository.findBySymbolAndSectorCode(symbol, sectorCode)
                .orElseGet(() -> symbolSectorMapRepository.save(new SymbolSectorMap(symbol, sectorCode, primary, weight)));
    }

    private void saveSectorProxyIfMissing(String sectorCode, String proxySymbol, String proxyType, BigDecimal weight) {
        sectorProxyRepository.findBySectorCodeAndProxySymbol(sectorCode, proxySymbol)
                .orElseGet(() -> sectorProxyRepository.save(new SectorProxy(sectorCode, proxySymbol, proxyType, weight)));
    }

    private String normalizeSectorCode(String sectorCode) {
        if (sectorCode == null || sectorCode.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "sectorCode must not be blank");
        }
        return sectorCode.trim().toUpperCase();
    }
}
