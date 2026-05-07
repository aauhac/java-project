package com.tradeagent.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LatestQuoteRepository extends JpaRepository<LatestQuote, Long> {

    Optional<LatestQuote> findBySymbol(String symbol);
}
