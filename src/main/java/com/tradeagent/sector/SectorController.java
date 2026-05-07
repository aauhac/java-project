package com.tradeagent.sector;

import com.tradeagent.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorAnalysisService sectorAnalysisService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;

    public SectorController(SectorAnalysisService sectorAnalysisService,
                            PortfolioSectorDiagnosticService portfolioSectorDiagnosticService) {
        this.sectorAnalysisService = sectorAnalysisService;
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
}
