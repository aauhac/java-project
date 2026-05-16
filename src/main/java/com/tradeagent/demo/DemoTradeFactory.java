package com.tradeagent.demo;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.TradeType;
import com.tradeagent.portfolio.TradeHistory;
import com.tradeagent.portfolio.TradeHistoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Disabled: using seed-based DataInitializer instead
// @Component
public class DemoTradeFactory {

    private final TradeHistoryRepository tradeHistoryRepository;

    public DemoTradeFactory(TradeHistoryRepository tradeHistoryRepository) {
        this.tradeHistoryRepository = tradeHistoryRepository;
    }

    @Transactional
    public void seed(Long userId) {
        if (!tradeHistoryRepository.findByUserId(userId).isEmpty()) {
            return;
        }

        LocalDate today = DateTimeUtil.today();
        List<TradeHistory> trades = List.of(
                trade(userId, "NVDA", "SEMI", TradeType.BUY, "780.0000", 2, tradingDateTime(today, 32)),
                trade(userId, "TSLA", "EV", TradeType.BUY, "225.0000", 3, tradingDateTime(today, 24)),
                trade(userId, "MSFT", "AIINF", TradeType.BUY, "405.0000", 2, tradingDateTime(today, 27)),
                trade(userId, "MSFT", "AIINF", TradeType.SELL, "412.0000", 1, tradingDateTime(today, 15)),
                trade(userId, "MRNA", "BIO", TradeType.BUY, "110.0000", 8, tradingDateTime(today, 30)),
                trade(userId, "MRNA", "BIO", TradeType.SELL, "88.0000", 4, tradingDateTime(today, 6))
        );

        tradeHistoryRepository.saveAll(trades);
    }

    private TradeHistory trade(Long userId, String symbol, String sectorCode, TradeType tradeType,
                               String price, int quantity, LocalDateTime tradedAt) {
        return new TradeHistory(
                userId,
                symbol,
                sectorCode,
                tradeType,
                new BigDecimal(price).setScale(4, RoundingMode.HALF_UP),
                quantity,
                tradedAt
        );
    }

    private LocalDateTime tradingDateTime(LocalDate today, int daysBefore) {
        return DateTimeUtil.tradingDaysBefore(today, daysBefore).atTime(14, 30);
    }
}
