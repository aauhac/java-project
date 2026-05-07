package com.tradeagent.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceBarRepository extends JpaRepository<PriceBar, Long> {

    List<PriceBar> findBySymbolAndBarTimeBetween(String symbol, LocalDateTime start, LocalDateTime end);

    Optional<PriceBar> findBySymbolAndBarTime(String symbol, LocalDateTime barTime);

    boolean existsBySymbolAndBarTime(String symbol, LocalDateTime barTime);
}
