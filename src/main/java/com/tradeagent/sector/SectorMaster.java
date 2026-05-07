package com.tradeagent.sector;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "sector_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sector_master_code", columnNames = "sector_code")
        },
        indexes = {
                @Index(name = "idx_sector_master_code", columnList = "sector_code")
        }
)
public class SectorMaster extends BaseEntity {

    @Column(name = "sector_code", nullable = false, length = 16)
    private String sectorCode;

    @Column(name = "sector_name", nullable = false, length = 64)
    private String sectorName;

    @Column(nullable = false, length = 500)
    private String description;

    protected SectorMaster() {
    }

    public SectorMaster(String sectorCode, String sectorName, String description) {
        this.sectorCode = sectorCode;
        this.sectorName = sectorName;
        this.description = description;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public String getSectorName() {
        return sectorName;
    }

    public String getDescription() {
        return description;
    }
}
