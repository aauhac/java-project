package com.tradeagent.sector;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "symbol_sector_map",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_symbol_sector_map_symbol_code", columnNames = {"symbol", "sector_code"})
        },
        indexes = {
                @Index(name = "idx_symbol_sector_map_symbol", columnList = "symbol"),
                @Index(name = "idx_symbol_sector_map_sector_code", columnList = "sector_code")
        }
)
public class SymbolSectorMap extends BaseEntity {

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "sector_code", nullable = false, length = 16)
    private String sectorCode;

    @Column(name = "is_primary", nullable = false)
    private Boolean primaryMapping;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weight;

    protected SymbolSectorMap() {
    }

    public SymbolSectorMap(String symbol, String sectorCode, Boolean primaryMapping, BigDecimal weight) {
        this.symbol = symbol;
        this.sectorCode = sectorCode;
        this.primaryMapping = primaryMapping;
        this.weight = weight;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public Boolean getPrimaryMapping() {
        return primaryMapping;
    }

    public BigDecimal getWeight() {
        return weight;
    }
}
