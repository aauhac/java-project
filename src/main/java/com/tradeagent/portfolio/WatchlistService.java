package com.tradeagent.portfolio;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.market.LatestQuote;
import com.tradeagent.market.MarketDataService;
import com.tradeagent.market.PriceBar;
import com.tradeagent.portfolio.PortfolioApiModels.WatchlistDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final MarketDataService marketDataService;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            MarketDataService marketDataService) {
        this.watchlistRepository = watchlistRepository;
        this.marketDataService = marketDataService;
    }

    @Transactional
    public WatchlistDto addWatchlist(Long userId, String symbol, LocalDate watchStartDate) {
        validateUserId(userId);

        String resolvedSymbol = normalizeSymbol(symbol);
        LocalDate resolvedStartDate = watchStartDate != null ? watchStartDate : LocalDate.now();

        WatchlistItem item = watchlistRepository.findByUserIdAndSymbol(userId, resolvedSymbol)
                .map(existing -> {
                    existing.updateWatchStartDate(resolvedStartDate);
                    return watchlistRepository.save(existing);
                })
                .orElseGet(() -> watchlistRepository.save(
                        new WatchlistItem(userId, resolvedSymbol, resolvedStartDate)
                ));

        return toDto(item);
    }

    @Transactional
    public void removeWatchlist(Long userId, String symbol) {
        validateUserId(userId);

        String resolvedSymbol = normalizeSymbol(symbol);

        if (!watchlistRepository.existsByUserIdAndSymbol(userId, resolvedSymbol)) {
            throw new NotFoundException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "watchlist item not found for symbol " + resolvedSymbol
            );
        }

        watchlistRepository.deleteByUserIdAndSymbol(userId, resolvedSymbol);
    }

    @Transactional
    public void removeWatchlistItems(Long userId, List<String> symbols) {
        validateUserId(userId);

        if (symbols == null || symbols.isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbols must not be empty");
        }

        for (String symbol : symbols) {
            String resolvedSymbol = normalizeSymbol(symbol);

            if (watchlistRepository.existsByUserIdAndSymbol(userId, resolvedSymbol)) {
                watchlistRepository.deleteByUserIdAndSymbol(userId, resolvedSymbol);
            }
        }
    }

    public List<WatchlistDto> getWatchlist(Long userId) {
        validateUserId(userId);

        return watchlistRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(WatchlistItem::getSymbol))
                .map(this::toDto)
                .toList();
    }

    private WatchlistDto toDto(WatchlistItem item) {
        LocalDate baseDate = resolveBaseDate(item);
        WatchlistPriceSnapshot snapshot = calculatePriceSnapshot(item.getSymbol(), baseDate);

        return new WatchlistDto(
                item.getSymbol(),
                baseDate,
                snapshot.basePrice(),
                snapshot.currentPrice(),
                snapshot.changeRate()
        );
    }

    private LocalDate resolveBaseDate(WatchlistItem item) {
        if (item.getWatchStartDate() != null) {
            return item.getWatchStartDate();
        }

        if (item.getCreatedAt() != null) {
            return item.getCreatedAt().toLocalDate();
        }

        return LocalDate.now();
    }

    private WatchlistPriceSnapshot calculatePriceSnapshot(String symbol, LocalDate baseDate) {
        LatestQuote latestQuote = marketDataService.getLatestQuote(symbol);
        BigDecimal currentPrice = scalePrice(latestQuote.getLastPrice());
        BigDecimal basePrice = scalePrice(findBaseClosePrice(symbol, baseDate));

        BigDecimal changeRate = calculateChangeRate(basePrice, currentPrice);

        return new WatchlistPriceSnapshot(basePrice, currentPrice, changeRate);
    }

    private BigDecimal calculateChangeRate(BigDecimal basePrice, BigDecimal currentPrice) {
        if (currentPrice == null
                || basePrice == null
                || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return currentPrice.subtract(basePrice)
                .divide(basePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal findBaseClosePrice(String symbol, LocalDate baseDate) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = baseDate.isAfter(today) ? baseDate : today;

        List<PriceBar> bars = marketDataService.getHistoricalBars(symbol, baseDate, endDate);

        return bars.stream()
                .filter(bar -> bar.getBarTime() != null)
                .filter(bar -> !bar.getBarTime().toLocalDate().isBefore(baseDate))
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .map(PriceBar::getClosePrice)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal scalePrice(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbol must not be blank");
        }

        return symbol.trim().toUpperCase();
    }

    private record WatchlistPriceSnapshot(
            BigDecimal basePrice,
            BigDecimal currentPrice,
            BigDecimal changeRate
    ) {
    }
}