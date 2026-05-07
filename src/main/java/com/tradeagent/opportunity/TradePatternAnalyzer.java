package com.tradeagent.opportunity;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.TradeType;
import com.tradeagent.common.ValidationException;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import com.tradeagent.portfolio.TradeHistory;
import com.tradeagent.portfolio.TradeHistoryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class TradePatternAnalyzer {

    private static final BigDecimal SOLD_TOO_EARLY_THRESHOLD = BigDecimal.valueOf(7);
    private static final BigDecimal HELD_TOO_LONG_THRESHOLD = BigDecimal.valueOf(-5);

    private final TradeHistoryRepository tradeHistoryRepository;
    private final PriceBarRepository priceBarRepository;

    public TradePatternAnalyzer(TradeHistoryRepository tradeHistoryRepository,
                                PriceBarRepository priceBarRepository) {
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.priceBarRepository = priceBarRepository;
    }

    public List<BetterTimingDto> findSoldTooEarly(Long userId) {
        long resolvedUserId = validateUserId(userId);
        return tradeHistoryRepository.findByUserId(resolvedUserId).stream()
                .filter(trade -> trade.getTradeType() == TradeType.SELL)
                .map(this::toSoldTooEarly)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(BetterTimingDto::changeRate).reversed())
                .toList();
    }

    public List<BetterTimingDto> findHeldTooLong(Long userId) {
        long resolvedUserId = validateUserId(userId);
        return tradeHistoryRepository.findByUserId(resolvedUserId).stream()
                .filter(trade -> trade.getTradeType() == TradeType.BUY)
                .map(this::toHeldTooLong)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(BetterTimingDto::changeRate))
                .toList();
    }

    public List<BetterTimingDto> suggestBetterEntryTiming(Long userId) {
        long resolvedUserId = validateUserId(userId);
        return tradeHistoryRepository.findByUserId(resolvedUserId).stream()
                .filter(trade -> trade.getTradeType() == TradeType.BUY)
                .map(this::toBetterEntrySuggestion)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<BetterTimingDto> suggestBetterExitTiming(Long userId) {
        return findSoldTooEarly(validateUserId(userId)).stream()
                .map(item -> new BetterTimingDto(
                        "BETTER_EXIT",
                        item.symbol(),
                        item.sectorCode(),
                        item.baseDate(),
                        item.actualPrice(),
                        item.referencePrice(),
                        item.changeRate(),
                        "A later exit could have captured more upside.",
                        null
                ))
                .toList();
    }

    private Optional<BetterTimingDto> toSoldTooEarly(TradeHistory sellTrade) {
        List<PriceBar> barsAfterSell = loadBarsAfterDate(sellTrade.getSymbol(), sellTrade.getTradedAt().toLocalDate(), 10);
        if (barsAfterSell.size() < 2) {
            return Optional.empty();
        }

        BigDecimal peakPrice = barsAfterSell.stream()
                .map(PriceBar::getHighPrice)
                .max(Comparator.naturalOrder())
                .orElse(sellTrade.getPrice());
        BigDecimal upside = calculatePercentChange(sellTrade.getPrice(), peakPrice);
        if (upside.compareTo(SOLD_TOO_EARLY_THRESHOLD) < 0) {
            return Optional.empty();
        }

        return Optional.of(new BetterTimingDto(
                "SOLD_TOO_EARLY",
                sellTrade.getSymbol(),
                sellTrade.getSectorCode(),
                sellTrade.getTradedAt().toLocalDate(),
                sellTrade.getPrice().setScale(4, RoundingMode.HALF_UP),
                peakPrice.setScale(4, RoundingMode.HALF_UP),
                upside,
                "The stock continued to rally after the sell.",
                null
        ));
    }

    private Optional<BetterTimingDto> toHeldTooLong(TradeHistory buyTrade) {
        Optional<TradeHistory> matchingSell = findMatchingSell(buyTrade);
        if (matchingSell.isEmpty()) {
            return Optional.empty();
        }

        TradeHistory sellTrade = matchingSell.get();
        List<PriceBar> holdingBars = loadBarsBetweenDates(
                buyTrade.getSymbol(),
                buyTrade.getTradedAt().toLocalDate(),
                sellTrade.getTradedAt().toLocalDate()
        );
        if (holdingBars.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal minLow = holdingBars.stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(buyTrade.getPrice());
        BigDecimal drawdown = calculatePercentChange(buyTrade.getPrice(), minLow);
        BigDecimal realizedReturn = calculatePercentChange(buyTrade.getPrice(), sellTrade.getPrice());
        if (drawdown.compareTo(HELD_TOO_LONG_THRESHOLD) > 0 || realizedReturn.compareTo(HELD_TOO_LONG_THRESHOLD) > 0) {
            return Optional.empty();
        }

        return Optional.of(new BetterTimingDto(
                "HELD_TOO_LONG",
                buyTrade.getSymbol(),
                buyTrade.getSectorCode(),
                sellTrade.getTradedAt().toLocalDate(),
                sellTrade.getPrice().setScale(4, RoundingMode.HALF_UP),
                minLow.setScale(4, RoundingMode.HALF_UP),
                realizedReturn,
                "A significant drawdown developed before the position was closed.",
                null
        ));
    }

    private Optional<BetterTimingDto> toBetterEntrySuggestion(TradeHistory buyTrade) {
        List<PriceBar> barsAfterBuy = loadBarsAfterDate(buyTrade.getSymbol(), buyTrade.getTradedAt().toLocalDate(), 10);
        if (barsAfterBuy.size() < 2) {
            return Optional.empty();
        }

        BigDecimal lowestPrice = barsAfterBuy.stream()
                .map(PriceBar::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(buyTrade.getPrice());
        BigDecimal improvement = calculatePercentChange(buyTrade.getPrice(), lowestPrice);
        if (improvement.compareTo(HELD_TOO_LONG_THRESHOLD) > 0) {
            return Optional.empty();
        }

        return Optional.of(new BetterTimingDto(
                "BETTER_ENTRY",
                buyTrade.getSymbol(),
                buyTrade.getSectorCode(),
                buyTrade.getTradedAt().toLocalDate(),
                buyTrade.getPrice().setScale(4, RoundingMode.HALF_UP),
                lowestPrice.setScale(4, RoundingMode.HALF_UP),
                improvement,
                "Waiting for a lower entry price would have improved the position.",
                null
        ));
    }

    private Optional<TradeHistory> findMatchingSell(TradeHistory buyTrade) {
        return tradeHistoryRepository.findByUserIdAndSymbol(buyTrade.getUserId(), buyTrade.getSymbol()).stream()
                .filter(trade -> trade.getTradeType() == TradeType.SELL)
                .filter(trade -> !trade.getTradedAt().isBefore(buyTrade.getTradedAt()))
                .min(Comparator.comparing(TradeHistory::getTradedAt));
    }

    private List<PriceBar> loadBarsAfterDate(String symbol, LocalDate baseDate, int days) {
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

    private List<PriceBar> loadBarsBetweenDates(String symbol, LocalDate from, LocalDate to) {
        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        symbol,
                        from.atStartOfDay(),
                        to.atTime(LocalTime.MAX))
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .toList();
    }

    private BigDecimal calculatePercentChange(BigDecimal basePrice, BigDecimal targetPrice) {
        if (basePrice == null || targetPrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return targetPrice.subtract(basePrice)
                .divide(basePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private long validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
        return userId;
    }
}
