package com.tradeagent.sector;

import com.tradeagent.common.ApiResponse;
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
    private final SectorTrendAnalysisService sectorTrendAnalysisService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;

    public SectorController(SectorAnalysisService sectorAnalysisService,
                            SectorTrendAnalysisService sectorTrendAnalysisService,
                            PortfolioSectorDiagnosticService portfolioSectorDiagnosticService) {
        this.sectorAnalysisService = sectorAnalysisService;
        this.sectorTrendAnalysisService = sectorTrendAnalysisService;
        this.portfolioSectorDiagnosticService = portfolioSectorDiagnosticService;
    }

    @GetMapping("/scores")
    public ApiResponse<List<SectorScoreDto>> getScores() {
        return ApiResponse.ok(sectorAnalysisService.getLatestSectorScores());
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

    @PostMapping("/analyze-trend")
    public ApiResponse<List<SectorTrendDto>> analyzeTrend(@RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(sectorTrendAnalysisService.analyze(date));
    }

    @GetMapping("/trends")
    public ApiResponse<List<SectorTrendDto>> getTrends(@RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(sectorTrendAnalysisService.getTrendScores(date));
    }

    @GetMapping("/{sectorCode}/trends")
    public ApiResponse<List<SectorTrendDto>> getSectorTrend(@PathVariable String sectorCode,
                                                            @RequestParam(required = false) LocalDate from,
                                                            @RequestParam(required = false) LocalDate to) {
        return ApiResponse.ok(sectorTrendAnalysisService.getSectorTrend(sectorCode, from, to));
    }

    @GetMapping("/user/{userId}/trend-match")
    public ApiResponse<PortfolioTrendMatchDto> getTrendMatch(@PathVariable Long userId,
                                                             @RequestParam(required = false) LocalDate date) {
        return ApiResponse.ok(portfolioSectorDiagnosticService.calculateTrendMatch(userId, date));
    }
}
