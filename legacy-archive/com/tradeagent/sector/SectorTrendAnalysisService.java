package com.tradeagent.sector;

import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Deprecated(forRemoval = false)
@Transactional(readOnly = true)
public class SectorTrendAnalysisService {

    private final SectorGkgTrendService sectorGkgTrendService;

    public SectorTrendAnalysisService(SectorGkgTrendService sectorGkgTrendService) {
        this.sectorGkgTrendService = sectorGkgTrendService;
    }

    @Transactional
    public List<SectorTrendDto> analyzeToday() {
        return getTrendScores(null);
    }

    @Transactional
    public List<SectorTrendDto> analyze(LocalDate date) {
        return getTrendScores(date);
    }

    @Transactional
    public List<SectorTrendDto> refreshTrendAnalysis(LocalDate date, boolean forceRefresh) {
        // 분석 버튼은 DB 조회만 수행하고, forceRefresh는 무시한다.
        return getTrendScores(date);
    }

    public List<SectorTrendDto> getTrendScores(LocalDate date) {
        return sectorGkgTrendService.getTrendScoresForDate(date);
    }

    public List<SectorTrendDto> getSectorTrend(String sectorCode, LocalDate from, LocalDate to) {
        String resolvedSectorCode = sectorCode == null ? "" : sectorCode.trim().toUpperCase(Locale.ROOT);
        return sectorGkgTrendService.getTrendScores(from, to).stream()
                .filter(item -> item.sectorCode().equals(resolvedSectorCode))
                .toList();
    }
}