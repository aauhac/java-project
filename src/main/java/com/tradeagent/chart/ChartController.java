package com.tradeagent.chart;

import com.tradeagent.chart.ChartModels.ChartResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;

@Controller
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/chart";
    }

    @GetMapping("/chart")
    public String chartPage() {
        return "redirect:/chart/index.html";
    }

    @GetMapping(path = "/chart/api/bars", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ChartResponse bars(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {
        return chartService.fetchBars(symbol, timeframe, start, end);
    }
}
