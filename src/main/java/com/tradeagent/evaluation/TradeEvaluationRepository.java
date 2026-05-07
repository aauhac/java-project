package com.tradeagent.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradeEvaluationRepository extends JpaRepository<TradeEvaluation, Long> {

    Optional<TradeEvaluation> findByTradeHistoryId(Long tradeHistoryId);
}
