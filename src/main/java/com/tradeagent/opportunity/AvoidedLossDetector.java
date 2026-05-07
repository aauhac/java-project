package com.tradeagent.opportunity;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.market.PriceBarRepository;
import com.tradeagent.portfolio.TradeHistoryRepository;
import com.tradeagent.portfolio.WatchlistItem;
import com.tradeagent.portfolio.WatchlistRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class AvoidedLossDetector extends AbstractOpportunityDetector<AvoidedLoss> {

    public AvoidedLossDetector(WatchlistRepository watchlistRepository,
                               TradeHistoryRepository tradeHistoryRepository,
                               PriceBarRepository priceBarRepository) {
        super(watchlistRepository, tradeHistoryRepository, priceBarRepository);
    }

    @Override
    public List<AvoidedLoss> detect(Long userId) {
        return getWatchlist(userId).stream()
                .filter(item -> isAvoidedLoss(item.getSymbol(), resolveBaseDate(item), userId))
                .map(this::toAvoidedLoss)
                .sorted(Comparator.comparing(AvoidedLoss::getAvoidedLossRate).reversed())
                .toList();
    }

    public boolean isAvoidedLoss(String symbol, java.time.LocalDate baseDate, Long userId) {
        BigDecimal downside = calculateMinReturn(symbol, baseDate, 10);
        return downside.compareTo(NEGATIVE_THRESHOLD) <= 0 && userDidNotBuy(userId, symbol, baseDate);
    }

    private AvoidedLoss toAvoidedLoss(WatchlistItem item) {
        java.time.LocalDate baseDate = resolveBaseDate(item);
        BigDecimal changeRate = calculateTenDayReturn(item.getSymbol(), baseDate);
        BigDecimal avoidedLossRate = calculateMinReturn(item.getSymbol(), baseDate, 10).abs();
        String reason = item.getSymbol() + " was on the watchlist, no buy was recorded, and the price fell within 10 trading days.";

        return new AvoidedLoss(
                item.getUserId(),
                item.getSymbol(),
                item.getSectorCode(),
                changeRate,
                avoidedLossRate,
                reason,
                DateTimeUtil.nowUtc()
        );
    }
}
