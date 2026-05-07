package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Component
public class PriceMomentumCalculator {

    private final SectorProxyRepository sectorProxyRepository;
    private final PriceBarRepository priceBarRepository;

    public PriceMomentumCalculator(SectorProxyRepository sectorProxyRepository, PriceBarRepository priceBarRepository) {
        this.sectorProxyRepository = sectorProxyRepository;
        this.priceBarRepository = priceBarRepository;
    }

    public BigDecimal calculatePriceMomentumScore(String sectorCode, LocalDate date) {
        List<SectorProxy> proxies = sectorProxyRepository.findBySectorCode(sectorCode);
        if (proxies.isEmpty()) {
            return neutralScore();
        }

        List<BigDecimal> returns = proxies.stream()
                .map(proxy -> calculateFiveDayReturn(proxy.getProxySymbol(), date))
                .filter(value -> value != null)
                .toList();

        if (returns.isEmpty()) {
            return neutralScore();
        }

        BigDecimal averageReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        if (averageReturn.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return BigDecimal.valueOf(90).setScale(2, RoundingMode.HALF_UP);
        }
        if (averageReturn.compareTo(BigDecimal.valueOf(2)) >= 0) {
            return BigDecimal.valueOf(70).setScale(2, RoundingMode.HALF_UP);
        }
        if (averageReturn.compareTo(BigDecimal.valueOf(-2)) >= 0) {
            return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateVolumeSpikeScore(String sectorCode, LocalDate date) {
        List<SectorProxy> proxies = sectorProxyRepository.findBySectorCode(sectorCode);
        if (proxies.isEmpty()) {
            return neutralScore();
        }

        List<BigDecimal> ratios = proxies.stream()
                .map(proxy -> calculateVolumeRatio(proxy.getProxySymbol(), date))
                .filter(value -> value != null)
                .toList();

        if (ratios.isEmpty()) {
            return neutralScore();
        }

        BigDecimal averageRatio = ratios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ratios.size()), 6, RoundingMode.HALF_UP);

        if (averageRatio.compareTo(BigDecimal.valueOf(2)) >= 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        if (averageRatio.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
            return BigDecimal.valueOf(80).setScale(2, RoundingMode.HALF_UP);
        }
        if (averageRatio.compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.valueOf(60).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(40).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateBreadthScore(String sectorCode, LocalDate date) {
        List<SectorProxy> proxies = sectorProxyRepository.findBySectorCode(sectorCode);
        if (proxies.isEmpty()) {
            return neutralScore();
        }

        long positiveCount = proxies.stream()
                .filter(proxy -> isAboveTwentyDayAverage(proxy.getProxySymbol(), date))
                .count();

        return BigDecimal.valueOf(positiveCount)
                .divide(BigDecimal.valueOf(proxies.size()), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFiveDayReturn(String symbol, LocalDate date) {
        List<PriceBar> bars = loadBars(symbol, date, 25);
        if (bars.size() < 6) {
            return null;
        }

        BigDecimal startPrice = bars.get(bars.size() - 6).getClosePrice();
        BigDecimal endPrice = bars.get(bars.size() - 1).getClosePrice();
        return percentChange(startPrice, endPrice);
    }

    private BigDecimal calculateVolumeRatio(String symbol, LocalDate date) {
        List<PriceBar> bars = loadBars(symbol, date, 25);
        if (bars.size() < 2) {
            return null;
        }

        PriceBar latest = bars.get(bars.size() - 1);
        List<PriceBar> history = bars.subList(0, bars.size() - 1);
        BigDecimal averageVolume = history.stream()
                .map(bar -> BigDecimal.valueOf(bar.getVolume()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 6, RoundingMode.HALF_UP);

        if (averageVolume.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        return BigDecimal.valueOf(latest.getVolume())
                .divide(averageVolume, 6, RoundingMode.HALF_UP);
    }

    private boolean isAboveTwentyDayAverage(String symbol, LocalDate date) {
        List<PriceBar> bars = loadBars(symbol, date, 25);
        if (bars.size() < 20) {
            return false;
        }

        PriceBar latest = bars.get(bars.size() - 1);
        List<PriceBar> latestTwenty = bars.subList(Math.max(0, bars.size() - 20), bars.size());
        BigDecimal averageClose = latestTwenty.stream()
                .map(PriceBar::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(latestTwenty.size()), 6, RoundingMode.HALF_UP);

        return latest.getClosePrice().compareTo(averageClose) >= 0;
    }

    private List<PriceBar> loadBars(String symbol, LocalDate date, int desiredBars) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();
        List<PriceBar> bars = priceBarRepository.findBySymbolAndBarTimeBetween(
                symbol,
                resolvedDate.minusDays(60).atStartOfDay(),
                resolvedDate.atTime(LocalTime.MAX));

        return bars.stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .skip(Math.max(0, bars.size() - desiredBars))
                .toList();
    }

    private BigDecimal percentChange(BigDecimal start, BigDecimal end) {
        if (start == null || end == null || start.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return end.subtract(start)
                .divide(start, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal neutralScore() {
        return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
    }
}
