package com.tradeagent.market;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarketDataService {

    private final LatestQuoteRepository latestQuoteRepository;
    private final MarketDataClient marketDataClient;
    private final HistoricalBarService historicalBarService;

    public MarketDataService(LatestQuoteRepository latestQuoteRepository,
                             MarketDataClient marketDataClient,
                             HistoricalBarService historicalBarService) {
        this.latestQuoteRepository = latestQuoteRepository;
        this.marketDataClient = marketDataClient;
        this.historicalBarService = historicalBarService;
    }

    public LatestQuote getLatestQuote(String symbol) {
        String resolvedSymbol = normalizeSymbol(symbol);
        return latestQuoteRepository.findBySymbol(resolvedSymbol)
                .orElseGet(() -> refreshLatestQuote(resolvedSymbol));
    }

    public List<PriceBar> getHistoricalBars(String symbol, LocalDate from, LocalDate to) {
        return historicalBarService.getHistoricalBars(symbol, from, to);
    }

    @Transactional
    public LatestQuote refreshLatestQuote(String symbol) {
        String resolvedSymbol = normalizeSymbol(symbol);
        LatestQuote fetchedQuote = marketDataClient.fetchLatestQuote(resolvedSymbol);

        return latestQuoteRepository.findBySymbol(resolvedSymbol)
                .map(existing -> {
                    existing.updateQuote(fetchedQuote.getLastPrice(), fetchedQuote.getChangeRate());
                    return latestQuoteRepository.save(existing);
                })
                .orElseGet(() -> latestQuoteRepository.save(fetchedQuote));
    }

    @Transactional
    public List<PriceBar> refreshHistoricalBars(String symbol, LocalDate from, LocalDate to) {
        return historicalBarService.refreshHistoricalBars(symbol, from, to);
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbol must not be blank");
        }
        return symbol.trim().toUpperCase();
    }
}
