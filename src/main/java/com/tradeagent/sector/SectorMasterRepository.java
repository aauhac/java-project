package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectorMasterRepository extends JpaRepository<SectorMaster, Long> {

    Optional<SectorMaster> findBySectorCode(String sectorCode);

    List<SectorMaster> findAllByOrderBySectorCodeAsc();
}
