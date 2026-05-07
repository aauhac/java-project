package com.tradeagent.portfolio;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.TradeType;
import com.tradeagent.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class TradeRegistrationService {

    private final PortfolioRepository portfolioRepository;
    private final TradeHistoryRepository tradeHistoryRepository;

    public TradeRegistrationService(PortfolioRepository portfolioRepository,
                                    TradeHistoryRepository tradeHistoryRepository) {
        this.portfolioRepository = portfolioRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
    }

    public PortfolioPosition registerBuy(TradeRequestDto request) {
        TradeRequest command = validateTradeRequest(request, true);

        PortfolioPosition position = portfolioRepository.findByUserIdAndSymbol(command.userId(), command.symbol())
                .map(existing -> {
                    existing.setSectorCode(command.sectorCode());
                    existing.increasePosition(command.price(), command.quantity());
                    return existing;
                })
                .orElseGet(() -> new PortfolioPosition(
                        command.userId(),
                        command.symbol(),
                        command.sectorCode(),
                        command.price().setScale(4, RoundingMode.HALF_UP),
                        command.quantity(),
                        command.price().multiply(BigDecimal.valueOf(command.quantity())).setScale(4, RoundingMode.HALF_UP)
                ));

        PortfolioPosition savedPosition = portfolioRepository.save(position);
        tradeHistoryRepository.save(new TradeHistory(
                command.userId(),
                command.symbol(),
                command.sectorCode(),
                TradeType.BUY,
                command.price().setScale(4, RoundingMode.HALF_UP),
                command.quantity(),
                DateTimeUtil.nowUtc()
        ));
        return savedPosition;
    }

    public void registerSell(TradeRequestDto request) {
        TradeRequest command = validateTradeRequest(request, false);

        PortfolioPosition position = portfolioRepository.findByUserIdAndSymbol(command.userId(), command.symbol())
                .orElseThrow(() -> new NotFoundException(ErrorCode.POSITION_NOT_FOUND,
                        "position not found for symbol " + command.symbol()));

        validateSellableQuantity(position, command.quantity());

        position.decreasePosition(command.quantity());
        if (StringUtils.hasText(command.sectorCode())) {
            position.setSectorCode(command.sectorCode());
        }

        if (position.getQuantity() == 0) {
            portfolioRepository.delete(position);
        } else {
            portfolioRepository.save(position);
        }

        tradeHistoryRepository.save(new TradeHistory(
                command.userId(),
                command.symbol(),
                StringUtils.hasText(command.sectorCode()) ? command.sectorCode() : position.getSectorCode(),
                TradeType.SELL,
                command.price().setScale(4, RoundingMode.HALF_UP),
                command.quantity(),
                DateTimeUtil.nowUtc()
        ));
    }

    public void validateSellableQuantity(PortfolioPosition position, int sellQuantity) {
        if (sellQuantity <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "quantity must be greater than zero");
        }
        if (position.getQuantity() < sellQuantity) {
            throw new ValidationException(ErrorCode.INSUFFICIENT_QUANTITY, "sell quantity exceeds held quantity");
        }
    }

    private TradeRequest validateTradeRequest(TradeRequestDto request, boolean sectorRequired) {
        if (request == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "request body is required");
        }
        if (request.userId() == null || request.userId() <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
        if (!StringUtils.hasText(request.symbol())) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbol must not be blank");
        }
        if (sectorRequired && !StringUtils.hasText(request.sectorCode())) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "sectorCode must not be blank");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "price must be greater than zero");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "quantity must be greater than zero");
        }

        return new TradeRequest(
                request.userId(),
                request.symbol().trim().toUpperCase(),
                StringUtils.hasText(request.sectorCode()) ? request.sectorCode().trim().toUpperCase() : null,
                request.price(),
                request.quantity()
        );
    }

    private record TradeRequest(Long userId, String symbol, String sectorCode, BigDecimal price, Integer quantity) {
    }
}
