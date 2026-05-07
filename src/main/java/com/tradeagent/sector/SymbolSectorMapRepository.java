package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SymbolSectorMapRepository extends JpaRepository<SymbolSectorMap, Long> {

    List<SymbolSectorMap> findBySymbol(String symbol);

    List<SymbolSectorMap> findBySectorCode(String sectorCode);

    Optional<SymbolSectorMap> findBySymbolAndSectorCode(String symbol, String sectorCode);
}
