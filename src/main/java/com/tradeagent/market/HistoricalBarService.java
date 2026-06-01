package com.tradeagent.market;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HistoricalBarService {

    private static final int DEFAULT_LIMIT = 60;

    private final PriceBarRepository priceBarRepository;
    private final MarketDataClient marketDataClient;

    public HistoricalBarService(PriceBarRepository priceBarRepository,
                                MarketDataClient marketDataClient) {
        this.priceBarRepository = priceBarRepository;
        this.marketDataClient = marketDataClient;
    }

    public List<PriceBar> getHistoricalBars(String symbol, LocalDate from, LocalDate to) {
        String resolvedSymbol = normalizeSymbol(symbol);
        DateRange range = resolveRange(from, to);
        List<PriceBar> storedBars = loadStoredBars(resolvedSymbol, range);
        if (!storedBars.isEmpty()) {
            return storedBars;
        }
        return refreshHistoricalBars(resolvedSymbol, range.from(), range.to());
    }

    @Transactional
    public List<PriceBar> refreshHistoricalBars(String symbol, LocalDate from, LocalDate to) {
        String resolvedSymbol = normalizeSymbol(symbol);
        DateRange range = resolveRange(from, to);

        List<PriceBar> fetchedBars = marketDataClient.fetchHistoricalBars(resolvedSymbol, range.from(), range.to());
        List<PriceBar> mergedBars = fetchedBars.stream()
                .map(bar -> priceBarRepository.findBySymbolAndBarTime(bar.getSymbol(), bar.getBarTime())
                        .map(existing -> {
                            existing.updateFrom(bar);
                            return existing;
                        })
                        .orElse(bar))
                .toList();

        priceBarRepository.saveAll(mergedBars);
        return loadStoredBars(resolvedSymbol, range);
    }

    private List<PriceBar> loadStoredBars(String symbol, DateRange range) {
        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        symbol,
                        range.from().atStartOfDay(),
                        range.to().atTime(LocalTime.MAX))
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .toList();
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to != null ? to : DateTimeUtil.today();
        int defaultSpan = Math.max(DEFAULT_LIMIT - 1, 30);
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(defaultSpan);

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }

        return new DateRange(resolvedFrom, resolvedTo);
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbol must not be blank");
        }
        return symbol.trim().toUpperCase();
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
