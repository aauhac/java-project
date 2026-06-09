package com.tradeagent.opportunity;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.TradeType;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import com.tradeagent.portfolio.TradeHistory;
import com.tradeagent.portfolio.TradeHistoryRepository;
import com.tradeagent.portfolio.WatchlistItem;
import com.tradeagent.portfolio.WatchlistRepository;
import com.tradeagent.opportunity.OpportunityApiModels.BetterTimingDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunityDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunitySummaryDto;
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
    private static final BigDecimal BIG_UP_RATE = BigDecimal.valueOf(10.0);
    private static final BigDecimal BIG_DOWN_RATE = BigDecimal.valueOf(-10.0);

    private final WatchlistRepository watchlistRepository;
    private final PriceBarRepository priceBarRepository;
    private final TradeHistoryRepository tradeHistoryRepository;

    public OpportunityAnalysisService(WatchlistRepository watchlistRepository,
                                      PriceBarRepository priceBarRepository,
                                      TradeHistoryRepository tradeHistoryRepository) {
        this.watchlistRepository = watchlistRepository;
        this.priceBarRepository = priceBarRepository;
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
        int missedCount = (int) cases.stream().filter(item -> "MISSED_OPPORTUNITY".equals(item.type())).count();
        int avoidedCount = (int) cases.stream().filter(item -> "AVOIDED_LOSS".equals(item.type())).count();

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

        LocalDate endDate = startDate.plusDays(WATCH_DAYS);

        List<PriceBar> bars = priceBarRepository.findBySymbolAndBarTimeBetween(
                        item.getSymbol(),
                        startDate.atStartOfDay(),
                        endDate.atTime(java.time.LocalTime.MAX))
                .stream()
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

        BigDecimal basePrice = bars.get(0).getClosePrice();

        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal maxRate = bars.stream()
                .map(PriceBar::getHighPrice)
                .filter(price -> price != null)
                .map(price -> pctChange(basePrice, price))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        BigDecimal minRate = bars.stream()
                .map(PriceBar::getLowPrice)
                .filter(price -> price != null)
                .map(price -> pctChange(basePrice, price))
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        java.util.ArrayList<OpportunityDto> results = new java.util.ArrayList<>();

        if (maxRate.compareTo(BIG_UP_RATE) >= 0) {
            BigDecimal score = opportunityScore(maxRate);
            results.add(new OpportunityDto(
                    "MISSED_OPPORTUNITY",
                    item.getSymbol(),
                    null,
                    maxRate,
                    score,
                    grade(score),
                    WATCH_DAYS,
                    "관심종목 등록 후 " + WATCH_DAYS + "일 내 +10% 이상 상승",
                    item.getSymbol() + "이(가) 관심종목 등록 후 " + WATCH_DAYS + "일 내 " + maxRate + "% 상승해 놓친 기회로 분류되었습니다.",
                    detectedAt
            ));
        }

        if (minRate.compareTo(BIG_DOWN_RATE) <= 0) {
            BigDecimal score = opportunityScore(minRate.abs());
            results.add(new OpportunityDto(
                    "AVOIDED_LOSS",
                    item.getSymbol(),
                    null,
                    minRate,
                    score,
                    grade(score),
                    WATCH_DAYS,
                    "관심종목 등록 후 " + WATCH_DAYS + "일 내 -10% 이하 하락",
                    item.getSymbol() + "이(가) 관심종목 등록 후 " + WATCH_DAYS + "일 내 " + minRate + "% 하락해 피한 위험으로 분류되었습니다.",
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
                .add(absRate.multiply(BigDecimal.valueOf(2)));

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