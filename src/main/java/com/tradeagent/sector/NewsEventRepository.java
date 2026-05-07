package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsEventRepository extends JpaRepository<NewsEvent, Long> {

    List<NewsEvent> findBySectorCodeOrderByPublishedAtDesc(String sectorCode);

    List<NewsEvent> findBySectorCodeAndPublishedAtBetweenOrderByPublishedAtDesc(String sectorCode,
                                                                                LocalDateTime start,
                                                                                LocalDateTime end);

    void deleteBySectorCodeAndPublishedAtBetween(String sectorCode, LocalDateTime start, LocalDateTime end);
}
