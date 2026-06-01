package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.List;

@NoRepositoryBean
@Deprecated(forRemoval = false)
public interface NewsEventRepository extends JpaRepository<NewsEvent, Long> {

    List<NewsEvent> findBySectorCodeOrderByPublishedAtDesc(String sectorCode);

    List<NewsEvent> findBySectorCodeAndPublishedAtBetweenOrderByPublishedAtDesc(String sectorCode,
                                                                                 LocalDateTime start,
                                                                                 LocalDateTime end);

    List<NewsEvent> findByPublishedAtBetweenOrderByPublishedAtDesc(LocalDateTime start, LocalDateTime end);

    void deleteBySectorCodeAndPublishedAtBetween(String sectorCode, LocalDateTime start, LocalDateTime end);

    void deleteByPublishedAtBetween(LocalDateTime start, LocalDateTime end);
}
