package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SectorNewsScoreRepository extends JpaRepository<SectorNewsScore, Long> {

    Optional<SectorNewsScore> findBySectorCodeAndScoreDate(String sectorCode, LocalDate scoreDate);

    Optional<SectorNewsScore> findTopBySectorCodeOrderByScoreDateDesc(String sectorCode);

    List<SectorNewsScore> findByScoreDateOrderByTotalSectorScoreDesc(LocalDate scoreDate);

    List<SectorNewsScore> findBySectorCodeAndScoreDateBetweenOrderByScoreDateAsc(String sectorCode, LocalDate from, LocalDate to);
}
