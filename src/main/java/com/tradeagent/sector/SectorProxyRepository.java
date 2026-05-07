package com.tradeagent.sector;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectorProxyRepository extends JpaRepository<SectorProxy, Long> {

    List<SectorProxy> findBySectorCode(String sectorCode);

    List<SectorProxy> findAllByOrderBySectorCodeAsc();

    Optional<SectorProxy> findBySectorCodeAndProxySymbol(String sectorCode, String proxySymbol);
}
