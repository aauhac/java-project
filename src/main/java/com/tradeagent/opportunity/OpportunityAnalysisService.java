package com.tradeagent.opportunity;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.TradeType;
import com.tradeagent.market.MarketDataService;
import com.tradeagent.market.PriceBar;
import com.tradeagent.opportunity.OpportunityApiModels.BetterTimingDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunityDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunitySummaryDto;
import com.tradeagent.portfolio.TradeHistory;
import com.tradeagent.portfolio.TradeHistoryRepository;
import com.tradeagent.portfolio.WatchlistItem;
import com.tradeagent.portfolio.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OpportunityAnalysisService {

    private static final int WATCH_DAYS = 10;
    private static final BigDecimal CLOSE_UP_RATE = BigDecimal.valueOf(10.0);
    private static final BigDecimal CLOSE_DOWN_RATE = BigDecimal.valueOf(-10.0);
    private static final BigDecimal SOFT_CLOSE_UP_RATE = BigDecimal.valueOf(5.0);
    private static final BigDecimal SOFT_CLOSE_DOWN_RATE = BigDecimal.valueOf(-5.0);
    private static final BigDecimal PEAK_UP_RATE = BigDecimal.valueOf(15.0);
    private static final BigDecimal DRAWDOWN_RATE = BigDecimal.valueOf(-15.0);

    private final WatchlistRepository watchlistRepository;
    private final MarketDataService marketDataService;
    private final TradeHistoryRepository tradeHistoryRepository;

    public OpportunityAnalysisService(WatchlistRepository watchlistRepository,
                                      MarketDataService marketDataService,
                                      TradeHistoryRepository tradeHistoryRepository) {
        this.watchlistRepository = watchlistRepository;
        this.marketDataService = marketDataService;
        this.tradeHistoryRepository = tradeHistoryRepository;
    }

    public List<OpportunityDto> getTopMissedOpportunities(Long userId) {
        return analyzeWatchlist(userId).stream()
                .filter(item -> "MISSED_OPPORTUNITY".equals(item.type()))
                .sorted(Comparator.comparing(OpportunityDto::changeRate).reversed())
                .limit(10)
                .toList();
    }

    public List<OpportunityDto> getTopAvoidedLosses(Long userId) {
        return analyzeWatchlist(userId).stream()
                .filter(item -> "AVOIDED_LOSS".equals(item.type()))
                .sorted((left, right) -> right.changeRate().abs().compareTo(left.changeRate().abs()))
                .limit(10)
                .toList();
    }

    public List<BetterTimingDto> getTradePatterns(Long userId) {
        return List.of();
    }

    public OpportunitySummaryDto getOpportunitySummary(Long userId) {
        List<OpportunityDto> cases = analyzeWatchlist(userId);

        int missedCount = (int) cases.stream()
                .filter(item -> "MISSED_OPPORTUNITY".equals(item.type()))
                .count();

        int avoidedCount = (int) cases.stream()
                .filter(item -> "AVOIDED_LOSS".equals(item.type()))
                .count();

        return new OpportunitySummaryDto(
                missedCount,
                avoidedCount,
                0,
                0
        );
    }

    private List<OpportunityDto> analyzeWatchlist(Long userId) {
        LocalDateTime detectedAt = DateTimeUtil.nowUtc();

        return watchlistRepository.findByUserId(userId).stream()
                .flatMap(item -> analyzeOne(item, userId, detectedAt).stream())
                .toList();
    }

    private List<OpportunityDto> analyzeOne(WatchlistItem item, Long userId, LocalDateTime detectedAt) {
        LocalDate startDate = item.getWatchStartDate() != null
                ? item.getWatchStartDate()
                : item.getCreatedAt() != null
                  ? item.getCreatedAt().toLocalDate()
                  : LocalDate.now();

        LocalDate plannedEndDate = startDate.plusDays(WATCH_DAYS);
        LocalDate today = LocalDate.now();
        LocalDate endDate = plannedEndDate.isAfter(today) ? today : plannedEndDate;

        List<PriceBar> bars = marketDataService.getHistoricalBars(item.getSymbol(), startDate, endDate).stream()
                .filter(bar -> bar.getBarTime() != null)
                .filter(bar -> !bar.getBarTime().toLocalDate().isBefore(startDate))
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .toList();

        if (bars.isEmpty()) {
            return List.of();
        }

        boolean boughtInWindow = tradeHistoryRepository.findByUserIdAndSymbol(userId, item.getSymbol()).stream()
                .anyMatch(trade -> trade.getTradeType() == TradeType.BUY && isWithinWatchWindow(trade, startDate, endDate));

        if (boughtInWindow) {
            return List.of();
        }

        BigDecimal basePrice = bars.stream()
                .map(PriceBar::getClosePrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);

        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal lastClose = bars.stream()
                .map(PriceBar::getClosePrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .reduce((first, second) -> second)
                .orElse(basePrice);

        BigDecimal peakPrice = bars.stream()
                .map(PriceBar::getHighPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(basePrice);

        BigDecimal lowPrice = bars.stream()
                .map(PriceBar::getLowPrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.naturalOrder())
                .orElse(basePrice);

        BigDecimal closeRate = pctChange(basePrice, lastClose);
        BigDecimal peakRate = pctChange(basePrice, peakPrice);
        BigDecimal drawdownRate = pctChange(basePrice, lowPrice);

        java.util.ArrayList<OpportunityDto> results = new java.util.ArrayList<>();

        boolean missedOpportunity = closeRate.compareTo(CLOSE_UP_RATE) >= 0
                || (closeRate.compareTo(SOFT_CLOSE_UP_RATE) >= 0 && peakRate.compareTo(PEAK_UP_RATE) >= 0);

        boolean avoidedLoss = closeRate.compareTo(CLOSE_DOWN_RATE) <= 0
                || (closeRate.compareTo(SOFT_CLOSE_DOWN_RATE) <= 0 && drawdownRate.compareTo(DRAWDOWN_RATE) <= 0);

        if (missedOpportunity) {
            BigDecimal score = opportunityScore(closeRate.max(peakRate));
            results.add(new OpportunityDto(
                    "MISSED_OPPORTUNITY",
                    item.getSymbol(),
                    null,
                    closeRate,
                    score,
                    grade(score),
                    WATCH_DAYS,
                    "관심종목 기준일 이후 종가 흐름 기준 상승",
                    item.getSymbol()
                            + "은(는) 기준일 이후 종가 기준 "
                            + closeRate
                            + "% 변화했습니다. 기간 중 최고 상승률은 "
                            + peakRate
                            + "%입니다.",
                    detectedAt
            ));
        }

        if (avoidedLoss) {
            BigDecimal score = opportunityScore(closeRate.abs().max(drawdownRate.abs()));
            results.add(new OpportunityDto(
                    "AVOIDED_LOSS",
                    item.getSymbol(),
                    null,
                    closeRate,
                    score,
                    grade(score),
                    WATCH_DAYS,
                    "관심종목 기준일 이후 종가 흐름 기준 하락",
                    item.getSymbol()
                            + "은(는) 기준일 이후 종가 기준 "
                            + closeRate
                            + "% 변화했습니다. 기간 중 최대 하락률은 "
                            + drawdownRate
                            + "%입니다.",
                    detectedAt
            ));
        }

        return results;
    }

    private boolean isWithinWatchWindow(TradeHistory trade, LocalDate startDate, LocalDate endDate) {
        LocalDate tradedDate = trade.getTradedAt().toLocalDate();
        return !tradedDate.isBefore(startDate) && !tradedDate.isAfter(endDate);
    }

    private BigDecimal pctChange(BigDecimal base, BigDecimal target) {
        if (base == null || target == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return target.subtract(base)
                .divide(base, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal opportunityScore(BigDecimal absRate) {
        BigDecimal score = BigDecimal.valueOf(40)
                .add(absRate.abs().multiply(BigDecimal.valueOf(2)));

        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private String grade(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "HIGH";
        }

        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "MEDIUM";
        }

        return "LOW";
    }
}