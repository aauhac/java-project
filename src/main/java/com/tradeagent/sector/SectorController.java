package com.tradeagent.sector;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.sector.SectorApiModels.RefreshNewsResultDto;
import com.tradeagent.sector.SectorApiModels.NewsEventDto;
import com.tradeagent.sector.SectorApiModels.PortfolioSectorDiagnosticDto;
import com.tradeagent.sector.SectorApiModels.PortfolioTrendMatchDto;
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
import java.util.List;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorAnalysisService sectorAnalysisService;
    private final SectorGkgTrendService sectorGkgTrendService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;

    public SectorController(SectorAnalysisService sectorAnalysisService,
                            SectorGkgTrendService sectorGkgTrendService,
                            PortfolioSectorDiagnosticService portfolioSectorDiagnosticService) {
        this.sectorAnalysisService = sectorAnalysisService;
        this.sectorGkgTrendService = sectorGkgTrendService;
        this.portfolioSectorDiagnosticService = portfolioSectorDiagnosticService;
    }

    @GetMapping("/scores")
    public ApiResponse<List<SectorScoreDto>> getScores() {
        return ApiResponse.ok(sectorAnalysisService.getLatestSectorScores());
    }

    @GetMapping("/masters")
    public ApiResponse<List<SectorOptionDto>> getSectorOptions() {
        return ApiResponse.ok(sectorAnalysisService.getSectorOptions());
    }

    @GetMapping("/{sectorCode}/score")
    public ApiResponse<SectorScoreDto> getSectorScore(@PathVariable String sectorCode) {
        return ApiResponse.ok(sectorAnalysisService.getSectorScore(sectorCode));
    }

    @GetMapping("/{sectorCode}/news")
    public ApiResponse<List<NewsEventDto>> getSectorNews(@PathVariable String sectorCode) {
        return ApiResponse.ok(sectorAnalysisService.getSectorNews(sectorCode));
    }

    @GetMapping("/user/{userId}/diagnostic")
    public ApiResponse<PortfolioSectorDiagnosticDto> getDiagnostic(@PathVariable Long userId) {
        return ApiResponse.ok(portfolioSectorDiagnosticService.diagnose(userId));
    }

    @PostMapping("/calculate")
    public ApiResponse<List<SectorScoreDto>> calculate() {
        return ApiResponse.ok(sectorAnalysisService.calculateTodaySectorScores());
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
}
