package com.tradeagent.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<PortfolioPosition, Long> {

    List<PortfolioPosition> findByUserId(Long userId);

    Optional<PortfolioPosition> findByUserIdAndSymbol(Long userId, String symbol);
}
