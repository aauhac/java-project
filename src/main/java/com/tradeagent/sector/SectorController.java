package com.tradeagent.sector;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.sector.SectorApiModels.NewsEventDto;
import com.tradeagent.sector.SectorApiModels.PortfolioSectorDiagnosticDto;
import com.tradeagent.sector.SectorApiModels.PortfolioTrendMatchDto;
import com.tradeagent.sector.SectorApiModels.RefreshNewsResultDto;
import com.tradeagent.sector.SectorApiModels.SectorOptionDto;
import com.tradeagent.sector.SectorApiModels.SectorScoreDto;
import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorGkgTrendService sectorGkgTrendService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;

    public SectorController(SectorGkgTrendService sectorGkgTrendService,
                            PortfolioSectorDiagnosticService portfolioSectorDiagnosticService) {
        this.sectorGkgTrendService = sectorGkgTrendService;
        this.portfolioSectorDiagnosticService = portfolioSectorDiagnosticService;
    }

    @GetMapping("/scores")
    public ApiResponse<List<SectorScoreDto>> getScores() {
        List<SectorScoreDto> scores = sectorGkgTrendService.getTrendScoresForDate(null).stream()
                .map(this::toScoreDto)
                .sorted(Comparator.comparing(SectorScoreDto::totalSectorScore).reversed())
                .toList();
        return ApiResponse.ok(scores);
    }

    @GetMapping("/masters")
    public ApiResponse<List<SectorOptionDto>> getSectorOptions() {
        List<SectorOptionDto> options = SectorConstants.SUPPORTED_SECTORS.stream()
                .map(code -> new SectorOptionDto(code, SectorConstants.nameOf(code)))
                .toList();
        return ApiResponse.ok(options);
    }

    @GetMapping("/{sectorCode}/score")
    public ApiResponse<SectorScoreDto> getSectorScore(@PathVariable String sectorCode) {
        String resolvedSectorCode = sectorCode == null ? "" : sectorCode.trim().toUpperCase(java.util.Locale.ROOT);
        SectorScoreDto score = sectorGkgTrendService.getTrendScoresForDate(null).stream()
                .filter(item -> item.sectorCode().equals(resolvedSectorCode))
                .map(this::toScoreDto)
                .findFirst()
                .orElse(new SectorScoreDto(
                        resolvedSectorCode,
                        SectorConstants.nameOf(resolvedSectorCode),
                        null,
                        java.math.BigDecimal.ZERO.setScale(2),
                        java.math.BigDecimal.ZERO.setScale(2),
                        java.math.BigDecimal.ZERO.setScale(2),
                        java.math.BigDecimal.ZERO.setScale(2),
                        java.math.BigDecimal.ZERO.setScale(2),
                        java.math.BigDecimal.ZERO.setScale(2),
                        "NEUTRAL"
                ));
        return ApiResponse.ok(score);
    }

    @GetMapping("/{sectorCode}/news")
    public ApiResponse<List<NewsEventDto>> getSectorNews(@PathVariable String sectorCode) {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/user/{userId}/diagnostic")
    public ApiResponse<PortfolioSectorDiagnosticDto> getDiagnostic(@PathVariable Long userId) {
        return ApiResponse.ok(portfolioSectorDiagnosticService.diagnose(userId));
    }

    @PostMapping("/calculate")
    public ApiResponse<List<SectorScoreDto>> calculate() {
        RefreshNewsResultDto refreshed = sectorGkgTrendService.refreshNews();
        return ApiResponse.ok(refreshed.trends().stream().map(this::toScoreDto).toList());
    }

    @PostMapping("/refresh-news")
    public ApiResponse<RefreshNewsResultDto> refreshNews() {
        return ApiResponse.ok(sectorGkgTrendService.refreshNews());
    }

    @GetMapping("/trends")
    public ApiResponse<List<SectorTrendDto>> getTrends(@RequestParam(required = false) LocalDate from,
                                                       @RequestParam(required = false) LocalDate to) {
        if (from == null && to == null) {
            return ApiResponse.ok(sectorGkgTrendService.getTrendScoresForDate(null));
        }
        return ApiResponse.ok(sectorGkgTrendService.getTrendScores(from, to));
    }

    @GetMapping("/{sectorCode}/trends")
    public ApiResponse<List<SectorTrendDto>> getSectorTrend(@PathVariable String sectorCode,
                                                            @RequestParam(required = false) LocalDate from,
                                                            @RequestParam(required = false) LocalDate to) {
        String resolvedSectorCode = sectorCode == null ? "" : sectorCode.trim().toUpperCase(java.util.Locale.ROOT);
        List<SectorTrendDto> items = sectorGkgTrendService.getTrendScores(from, to).stream()
                .filter(item -> item.sectorCode().equals(resolvedSectorCode))
                .toList();
        return ApiResponse.ok(items);
    }

    @GetMapping("/user/{userId}/trend-match")
    public ApiResponse<PortfolioTrendMatchDto> getTrendMatch(@PathVariable Long userId,
                                                             @RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(portfolioSectorDiagnosticService.calculateTrendMatch(userId, date));
    }

    private SectorScoreDto toScoreDto(SectorTrendDto trend) {
        return new SectorScoreDto(
                trend.sectorCode(),
                trend.sectorName(),
                trend.scoreDate(),
                trend.newsVolumeScore(),
                trend.newsToneScore(),
                java.math.BigDecimal.ZERO.setScale(2),
                java.math.BigDecimal.ZERO.setScale(2),
                java.math.BigDecimal.ZERO.setScale(2),
                trend.totalSectorScore(),
                trend.status()
        );
    }
}
