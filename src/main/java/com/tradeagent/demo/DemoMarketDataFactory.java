package com.tradeagent.demo;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.market.LatestQuote;
import com.tradeagent.market.LatestQuoteRepository;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoMarketDataFactory {

    private static final int PAST_TRADING_DAYS = 45;
    private static final int FUTURE_TRADING_DAYS = 10;

    private final PriceBarRepository priceBarRepository;
    private final LatestQuoteRepository latestQuoteRepository;

    public DemoMarketDataFactory(PriceBarRepository priceBarRepository,
                                 LatestQuoteRepository latestQuoteRepository) {
        this.priceBarRepository = priceBarRepository;
        this.latestQuoteRepository = latestQuoteRepository;
    }

    @Transactional
    public void seed() {
        LocalDate today = DateTimeUtil.today();
        List<LocalDate> tradingDates = buildTradingDates(
                DateTimeUtil.tradingDaysBefore(today, PAST_TRADING_DAYS),
                DateTimeUtil.tradingDaysAfter(today, FUTURE_TRADING_DAYS)
        );
        LocalDate currentQuoteDate = tradingDates.stream()
                .filter(date -> !date.isAfter(today))
                .max(Comparator.naturalOrder())
                .orElse(tradingDates.getFirst());

        for (Map.Entry<String, SymbolScenario> entry : scenarios().entrySet()) {
            SeededQuote seededQuote = seedBars(entry.getKey(), entry.getValue(), tradingDates, currentQuoteDate);
            upsertQuote(entry.getKey(), seededQuote.currentPrice(), seededQuote.changeRate());
        }
    }

    private SeededQuote seedBars(String symbol, SymbolScenario scenario, List<LocalDate> tradingDates, LocalDate currentQuoteDate) {
        BigDecimal previousClose = scenario.initialPrice().setScale(4, RoundingMode.HALF_UP);
        BigDecimal quoteClose = previousClose;
        BigDecimal previousQuoteClose = previousClose;

        int pastDateCount = (int) tradingDates.stream()
                .filter(date -> !date.isAfter(currentQuoteDate))
                .count();
        int recentStartIndex = Math.max(0, pastDateCount - 10);

        for (int index = 0; index < tradingDates.size(); index++) {
            LocalDate tradingDate = tradingDates.get(index);
            BigDecimal changePercent = resolveDailyChangePercent(index, pastDateCount, recentStartIndex, scenario);
            BigDecimal closePrice = applyPercent(previousClose, changePercent);
            BigDecimal openPrice = previousClose;
            BigDecimal band = BigDecimal.valueOf(0.010 + ((index % 4) * 0.003));
            BigDecimal highPrice = openPrice.max(closePrice)
                    .multiply(BigDecimal.ONE.add(band))
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal lowPrice = openPrice.min(closePrice)
                    .multiply(BigDecimal.ONE.subtract(band))
                    .setScale(4, RoundingMode.HALF_UP);
            long volume = resolveVolume(tradingDate, currentQuoteDate, scenario, index);

            LocalDateTime barTime = tradingDate.atTime(14, 30);
            if (!priceBarRepository.existsBySymbolAndBarTime(symbol, barTime)) {
                priceBarRepository.save(new PriceBar(
                        symbol,
                        barTime,
                        openPrice.setScale(4, RoundingMode.HALF_UP),
                        highPrice,
                        lowPrice,
                        closePrice,
                        volume
                ));
            }

            if (tradingDate.equals(currentQuoteDate)) {
                previousQuoteClose = previousClose;
                quoteClose = closePrice;
            }
            previousClose = closePrice;
        }

        BigDecimal changeRate = percentChange(previousQuoteClose, quoteClose);
        return new SeededQuote(quoteClose, changeRate);
    }

    private void upsertQuote(String symbol, BigDecimal currentPrice, BigDecimal changeRate) {
        latestQuoteRepository.findBySymbol(symbol)
                .map(existing -> {
                    existing.updateQuote(currentPrice, changeRate);
                    return latestQuoteRepository.save(existing);
                })
                .orElseGet(() -> latestQuoteRepository.save(new LatestQuote(symbol, currentPrice, changeRate)));
    }

    private BigDecimal resolveDailyChangePercent(int index, int pastDateCount, int recentStartIndex, SymbolScenario scenario) {
        if (index >= pastDateCount) {
            return BigDecimal.valueOf(scenario.futureTrendPercent());
        }
        if (index >= recentStartIndex) {
            return BigDecimal.valueOf(scenario.recentTrendPercent());
        }
        return BigDecimal.valueOf(scenario.earlyTrendPercent());
    }

    private long resolveVolume(LocalDate tradingDate, LocalDate currentQuoteDate, SymbolScenario scenario, int index) {
        double multiplier = 1.0 + ((index % 5) * 0.05);
        if (tradingDate.equals(currentQuoteDate)) {
            multiplier *= scenario.currentVolumeMultiplier();
        } else if (tradingDate.isAfter(currentQuoteDate)) {
            multiplier *= scenario.futureVolumeMultiplier();
        }
        return Math.round(scenario.baseVolume() * multiplier);
    }

    private BigDecimal applyPercent(BigDecimal basePrice, BigDecimal percent) {
        return basePrice.multiply(BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal basePrice, BigDecimal currentPrice) {
        if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return currentPrice.subtract(basePrice)
                .divide(basePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<LocalDate> buildTradingDates(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new java.util.ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            DayOfWeek dayOfWeek = cursor.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private Map<String, SymbolScenario> scenarios() {
        Map<String, SymbolScenario> scenarios = new LinkedHashMap<>();
        scenarios.put("NVDA", new SymbolScenario(new BigDecimal("720.00"), 0.35, 0.90, 0.20, 52_000_000L, 2.40, 1.10));
        scenarios.put("AMD", new SymbolScenario(new BigDecimal("165.00"), 0.25, 0.80, 0.75, 36_000_000L, 2.10, 1.15));
        scenarios.put("AVGO", new SymbolScenario(new BigDecimal("1380.00"), 0.18, 0.45, 0.80, 6_500_000L, 1.80, 1.10));
        scenarios.put("TSM", new SymbolScenario(new BigDecimal("155.00"), 0.18, 0.55, 0.18, 14_000_000L, 1.75, 1.05));
        scenarios.put("SMH", new SymbolScenario(new BigDecimal("235.00"), 0.16, 0.45, 0.15, 7_800_000L, 1.70, 1.05));
        scenarios.put("MSFT", new SymbolScenario(new BigDecimal("385.00"), 0.12, 0.55, 0.40, 24_000_000L, 1.65, 1.05));
        scenarios.put("AMZN", new SymbolScenario(new BigDecimal("188.00"), 0.10, 0.40, 0.70, 31_000_000L, 1.70, 1.10));
        scenarios.put("GOOGL", new SymbolScenario(new BigDecimal("162.00"), 0.08, 0.35, 0.30, 19_000_000L, 1.55, 1.05));
        scenarios.put("TSLA", new SymbolScenario(new BigDecimal("255.00"), -0.12, -0.55, -0.10, 44_000_000L, 1.20, 1.00));
        scenarios.put("RIVN", new SymbolScenario(new BigDecimal("14.50"), -0.20, -0.70, -0.85, 27_000_000L, 1.10, 1.00));
        scenarios.put("LI", new SymbolScenario(new BigDecimal("29.00"), -0.05, -0.35, -0.08, 8_000_000L, 1.15, 1.00));
        scenarios.put("MRNA", new SymbolScenario(new BigDecimal("120.00"), -0.25, -0.20, 0.05, 5_500_000L, 1.30, 1.00));
        scenarios.put("GILD", new SymbolScenario(new BigDecimal("79.00"), 0.05, 0.08, -0.04, 7_200_000L, 1.25, 1.00));
        scenarios.put("AMGN", new SymbolScenario(new BigDecimal("292.00"), 0.04, 0.10, 0.02, 3_100_000L, 1.20, 1.00));
        scenarios.put("IBB", new SymbolScenario(new BigDecimal("132.00"), 0.03, 0.12, 0.04, 1_900_000L, 1.15, 1.00));
        return scenarios;
    }

    private record SymbolScenario(BigDecimal initialPrice, double earlyTrendPercent, double recentTrendPercent,
                                  double futureTrendPercent, long baseVolume, double currentVolumeMultiplier,
                                  double futureVolumeMultiplier) {
    }

    private record SeededQuote(BigDecimal currentPrice, BigDecimal changeRate) {
    }
}
