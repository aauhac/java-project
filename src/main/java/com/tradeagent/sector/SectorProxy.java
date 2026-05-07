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
        name = "sector_proxy",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sector_proxy_code_symbol", columnNames = {"sector_code", "proxy_symbol"})
        },
        indexes = {
                @Index(name = "idx_sector_proxy_code", columnList = "sector_code")
        }
)
public class SectorProxy extends BaseEntity {

    @Column(name = "sector_code", nullable = false, length = 16)
    private String sectorCode;

    @Column(name = "proxy_symbol", nullable = false, length = 16)
    private String proxySymbol;

    @Column(name = "proxy_type", nullable = false, length = 16)
    private String proxyType;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weight;

    protected SectorProxy() {
    }

    public SectorProxy(String sectorCode, String proxySymbol, String proxyType, BigDecimal weight) {
        this.sectorCode = sectorCode;
        this.proxySymbol = proxySymbol;
        this.proxyType = proxyType;
        this.weight = weight;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public String getProxySymbol() {
        return proxySymbol;
    }

    public String getProxyType() {
        return proxyType;
    }

    public BigDecimal getWeight() {
        return weight;
    }
}
