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
public class MissedOpportunityDetector extends AbstractOpportunityDetector<MissedOpportunity> {

    public MissedOpportunityDetector(WatchlistRepository watchlistRepository,
                                     TradeHistoryRepository tradeHistoryRepository,
                                     PriceBarRepository priceBarRepository) {
        super(watchlistRepository, tradeHistoryRepository, priceBarRepository);
    }

    @Override
    public List<MissedOpportunity> detect(Long userId) {
        return getWatchlist(userId).stream()
                .filter(item -> isMissedOpportunity(item.getSymbol(), resolveBaseDate(item), userId))
                .map(this::toMissedOpportunity)
                .sorted(Comparator.comparing(MissedOpportunity::getExpectedReturn).reversed())
                .toList();
    }

    public boolean isMissedOpportunity(String symbol, java.time.LocalDate baseDate, Long userId) {
        BigDecimal upside = calculateMaxReturn(symbol, baseDate, 10);
        return upside.compareTo(POSITIVE_THRESHOLD) >= 0 && userDidNotBuy(userId, symbol, baseDate);
    }

    private MissedOpportunity toMissedOpportunity(WatchlistItem item) {
        java.time.LocalDate baseDate = resolveBaseDate(item);
        BigDecimal changeRate = calculateTenDayReturn(item.getSymbol(), baseDate);
        BigDecimal expectedReturn = calculateMaxReturn(item.getSymbol(), baseDate, 10);
        String reason = item.getSymbol() + " was on the watchlist, no buy was recorded, and the price rose within 10 trading days.";

        return new MissedOpportunity(
                item.getUserId(),
                item.getSymbol(),
                item.getSectorCode(),
                changeRate,
                expectedReturn,
                reason,
                DateTimeUtil.nowUtc()
        );
    }
}
