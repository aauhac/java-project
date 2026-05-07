package com.tradeagent.opportunity;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.TradeType;
import com.tradeagent.common.ValidationException;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import com.tradeagent.portfolio.TradeHistoryRepository;
import com.tradeagent.portfolio.WatchlistItem;
import com.tradeagent.portfolio.WatchlistRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractOpportunityDetector<T extends OpportunityCase> implements OpportunityDetector<T> {

    protected static final BigDecimal POSITIVE_THRESHOLD = BigDecimal.valueOf(7);
    protected static final BigDecimal NEGATIVE_THRESHOLD = BigDecimal.valueOf(-7);

    private final WatchlistRepository watchlistRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final PriceBarRepository priceBarRepository;

    protected AbstractOpportunityDetector(WatchlistRepository watchlistRepository,
                                          TradeHistoryRepository tradeHistoryRepository,
                                          PriceBarRepository priceBarRepository) {
        this.watchlistRepository = watchlistRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.priceBarRepository = priceBarRepository;
    }

    protected List<WatchlistItem> getWatchlist(Long userId) {
        return watchlistRepository.findByUserId(validateUserId(userId));
    }

    protected BigDecimal calculateTenDayReturn(String symbol, LocalDate baseDate) {
        List<PriceBar> bars = getBarsAfterDate(symbol, baseDate, 10);
        if (bars.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal basePrice = bars.getFirst().getClosePrice();
        BigDecimal finalPrice = bars.getLast().getClosePrice();
        return calculatePercentChange(basePrice, finalPrice);
    }

    protected boolean userDidNotBuy(Long userId, String symbol, LocalDate baseDate) {
        LocalDate endDate = baseDate.plusDays(20);
        return tradeHistoryRepository.findByUserIdAndSymbol(validateUserId(userId), symbol).stream()
                .filter(trade -> trade.getTradeType() == TradeType.BUY)
                .map(trade -> trade.getTradedAt().toLocalDate())
                .noneMatch(tradeDate -> !tradeDate.isBefore(baseDate) && !tradeDate.isAfter(endDate));
    }

    protected List<PriceBar> getBarsAfterDate(String symbol, LocalDate baseDate, int days) {
        if (baseDate == null) {
            return List.of();
        }

        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        symbol,
                        baseDate.atStartOfDay(),
                        baseDate.plusDays(Math.max(days * 3L, days)).atTime(LocalTime.MAX))
                .stream()
                .filter(bar -> !bar.getBarTime().toLocalDate().isBefore(baseDate))
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .limit(days + 1L)
                .toList();
    }

    protected BigDecimal calculateMaxReturn(String symbol, LocalDate baseDate, int days) {
        List<PriceBar> bars = getBarsAfterDate(symbol, baseDate, days);
        if (bars.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal basePrice = bars.getFirst().getClosePrice();
        BigDecimal peakPrice = bars.stream()
                .map(PriceBar::getHighPrice)
                .max(Comparator.naturalOrder())
                .orElse(basePrice);
        return calculatePercentChange(basePrice, peakPrice);
    }

    protected BigDecimal calculateMinReturn(String symbol, LocalDate baseDate, int days) {
        List<PriceBar> bars = getBarsAfterDate(symbol, baseDate, days);
        if (bars.size() < 2) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal basePrice = bars.getFirst().getClosePrice();
        BigDecimal lowestPrice = bars.stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(basePrice);
        return calculatePercentChange(basePrice, lowestPrice);
    }

    protected LocalDate resolveBaseDate(WatchlistItem item) {
        LocalDateTime createdAt = item.getCreatedAt();
        return createdAt != null ? createdAt.toLocalDate() : LocalDate.now();
    }

    protected BigDecimal calculatePercentChange(BigDecimal basePrice, BigDecimal targetPrice) {
        if (basePrice == null || targetPrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return targetPrice.subtract(basePrice)
                .divide(basePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    protected long validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
        return userId;
    }
}
