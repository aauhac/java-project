package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorScoreRepository extends JpaRepository<SectorScore, Long> {

    Optional<SectorScore> findBySectorCodeAndScoreDate(String sectorCode, LocalDate scoreDate);

    Optional<SectorScore> findTopBySectorCodeOrderByScoreDateDesc(String sectorCode);

    List<SectorScore> findByScoreDateOrderByTotalSectorScoreDesc(LocalDate scoreDate);

    List<SectorScore> findBySectorCodeAndScoreDateBetweenOrderByScoreDateAsc(String sectorCode, LocalDate from, LocalDate to);
}
