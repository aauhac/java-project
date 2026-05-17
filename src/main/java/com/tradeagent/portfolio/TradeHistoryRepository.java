package com.tradeagent.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {

    List<TradeHistory> findByUserId(Long userId);

    List<TradeHistory> findByUserIdAndSymbol(Long userId, String symbol);

    List<TradeHistory> findByUserIdOrderByTradedAtDesc(Long userId);
}
