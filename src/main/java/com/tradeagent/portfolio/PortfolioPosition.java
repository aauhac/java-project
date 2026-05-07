package com.tradeagent.portfolio;

import com.tradeagent.common.BaseEntity;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(
        name = "portfolio_positions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_portfolio_position_user_symbol", columnNames = {"user_id", "symbol"})
        },
        indexes = {
                @Index(name = "idx_portfolio_position_user_symbol", columnList = "user_id, symbol")
        }
)
public class PortfolioPosition extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "sector_code", nullable = false, length = 32)
    private String sectorCode;

    @Column(name = "avg_buy_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal avgBuyPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_buy_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalBuyAmount;

    protected PortfolioPosition() {
    }

    public PortfolioPosition(Long userId, String symbol, String sectorCode, BigDecimal avgBuyPrice, Integer quantity,
                             BigDecimal totalBuyAmount) {
        this.userId = userId;
        this.symbol = symbol;
        this.sectorCode = sectorCode;
        this.avgBuyPrice = avgBuyPrice;
        this.quantity = quantity;
        this.totalBuyAmount = totalBuyAmount;
    }

    public void increasePosition(BigDecimal buyPrice, int quantity) {
        validateTradeInput(buyPrice, quantity);
        BigDecimal tradeAmount = buyPrice.multiply(BigDecimal.valueOf(quantity));
        this.totalBuyAmount = this.totalBuyAmount.add(tradeAmount);
        this.quantity += quantity;
        this.avgBuyPrice = this.totalBuyAmount
                .divide(BigDecimal.valueOf(this.quantity), 4, RoundingMode.HALF_UP);
    }

    public void decreasePosition(int quantity) {
        validateQuantity(quantity);
        if (quantity > this.quantity) {
            throw new ValidationException(ErrorCode.INSUFFICIENT_QUANTITY, "sell quantity exceeds held quantity");
        }

        int remainingQuantity = this.quantity - quantity;
        if (remainingQuantity == 0) {
            this.quantity = 0;
            this.totalBuyAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            this.avgBuyPrice = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            return;
        }

        this.quantity = remainingQuantity;
        this.totalBuyAmount = this.avgBuyPrice.multiply(BigDecimal.valueOf(remainingQuantity))
                .setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMarketValue(BigDecimal currentPrice) {
        BigDecimal safePrice = currentPrice == null ? BigDecimal.ZERO : currentPrice;
        return safePrice.multiply(BigDecimal.valueOf(quantity)).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateProfitLoss(BigDecimal currentPrice) {
        return calculateMarketValue(currentPrice).subtract(totalBuyAmount).setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateReturnRate(BigDecimal currentPrice) {
        if (totalBuyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return calculateProfitLoss(currentPrice)
                .divide(totalBuyAmount, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateTradeInput(BigDecimal buyPrice, int quantity) {
        if (buyPrice == null || buyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "price must be greater than zero");
        }
        validateQuantity(quantity);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "quantity must be greater than zero");
        }
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public void setSectorCode(String sectorCode) {
        this.sectorCode = sectorCode;
    }

    public BigDecimal getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalBuyAmount() {
        return totalBuyAmount;
    }
}
