package com.tradeagent.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackLogRepository extends JpaRepository<FeedbackLog, Long> {

    List<FeedbackLog> findByUserId(Long userId);
}
