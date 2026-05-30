package com.tradeagent.sector;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.common.GdeltSupport;
import com.tradeagent.sector.SectorApiModels.NewsEventDto;
import com.tradeagent.sector.SectorApiModels.GdeltDebugResultDto;
import com.tradeagent.sector.SectorApiModels.PortfolioSectorDiagnosticDto;
import com.tradeagent.sector.SectorApiModels.PortfolioTrendMatchDto;
import com.tradeagent.sector.SectorApiModels.SectorOptionDto;
import com.tradeagent.sector.SectorApiModels.SectorScoreDto;
import com.tradeagent.sector.SectorApiModels.TrendAnalysisResultDto;
import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorAnalysisService sectorAnalysisService;
    private final SectorTrendAnalysisService sectorTrendAnalysisService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;
    private final GdeltClient gdeltClient;
    private final com.tradeagent.config.GdeltProperties gdeltProperties;

    public SectorController(SectorAnalysisService sectorAnalysisService,
                            SectorTrendAnalysisService sectorTrendAnalysisService,
                            PortfolioSectorDiagnosticService portfolioSectorDiagnosticService,
                            GdeltClient gdeltClient,
                            com.tradeagent.config.GdeltProperties gdeltProperties) {
        this.sectorAnalysisService = sectorAnalysisService;
        this.sectorTrendAnalysisService = sectorTrendAnalysisService;
        this.portfolioSectorDiagnosticService = portfolioSectorDiagnosticService;
        this.gdeltClient = gdeltClient;
        this.gdeltProperties = gdeltProperties;
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

    @PostMapping("/analyze-trend")
    public ApiResponse<List<SectorTrendDto>> analyzeTrend(@RequestParam(required = false) LocalDate date,
                                                          @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.ok(sectorTrendAnalysisService.getTrendScores(date));
    }

    @PostMapping("/user/{userId}/analyze-trend")
    public ApiResponse<TrendAnalysisResultDto> analyzeTrendForUser(@PathVariable Long userId,
                                                                    @RequestParam(required = false) LocalDate date,
                                                                    @RequestParam(defaultValue = "false") boolean force) {
        List<SectorTrendDto> trends = sectorTrendAnalysisService.getTrendScores(date);
        PortfolioTrendMatchDto trendMatch = portfolioSectorDiagnosticService.calculateTrendMatch(userId, date, trends);
        return ApiResponse.ok(new TrendAnalysisResultDto(trends, trendMatch));
    }

    @PostMapping("/refresh-news-trend")
    public ApiResponse<TrendAnalysisResultDto> refreshNewsTrend(@RequestParam(required = false) LocalDate date,
                                                                @RequestParam(defaultValue = "false") boolean force) {
        List<SectorTrendDto> trends = sectorTrendAnalysisService.refreshTrendAnalysis(date, force);
        PortfolioTrendMatchDto trendMatch = portfolioSectorDiagnosticService.calculateTrendMatch(1L, date, trends);
        return ApiResponse.ok(new TrendAnalysisResultDto(trends, trendMatch));
    }

    @PostMapping("/user/{userId}/refresh-news-trend")
    public ApiResponse<TrendAnalysisResultDto> refreshNewsTrendForUser(@PathVariable Long userId,
                                                                       @RequestParam(required = false) LocalDate date,
                                                                       @RequestParam(defaultValue = "false") boolean force) {
        List<SectorTrendDto> trends = sectorTrendAnalysisService.refreshTrendAnalysis(date, force);
        PortfolioTrendMatchDto trendMatch = portfolioSectorDiagnosticService.calculateTrendMatch(userId, date, trends);
        return ApiResponse.ok(new TrendAnalysisResultDto(trends, trendMatch));
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

    @GetMapping("/gdelt-status")
    public ApiResponse<GdeltSupport.StatusSnapshot> getGdeltStatus() {
        return ApiResponse.ok(GdeltSupport.snapshot(
                GdeltSupport.defaultLastRequestFile(gdeltProperties.getLastRequestFile()),
                GdeltSupport.defaultCacheDir(gdeltProperties.getCacheDir()),
                gdeltProperties.getMinRequestIntervalMs(),
                Duration.ofHours(Math.max(gdeltProperties.getCacheTtlHours(), 1))
        ));
    }

    @GetMapping("/gdelt-test")
    public ApiResponse<GdeltDebugResultDto> testGdelt(@RequestParam String query,
                                                      @RequestParam(required = false) LocalDate from,
                                                      @RequestParam(required = false) LocalDate to,
                                                      @RequestParam(defaultValue = "5") int maxRecords,
                                                      @RequestParam(defaultValue = "false") boolean force) {
        List<NewsEventDto> items = gdeltClient.fetchByQuery(query, from, to, maxRecords, force).stream()
                .map(event -> new NewsEventDto(
                        event.getSectorCode(),
                        event.getSymbol(),
                        event.getTitle(),
                        event.getSource(),
                        event.getUrl(),
                        event.getToneScore(),
                        event.getPublishedAt()
                ))
                .toList();

        return ApiResponse.ok(new GdeltDebugResultDto(
                query,
                from,
                to,
                maxRecords,
                items.size(),
                items
        ));
    }
}
