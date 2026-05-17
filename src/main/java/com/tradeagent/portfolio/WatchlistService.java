package com.tradeagent.portfolio;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.market.LatestQuote;
import com.tradeagent.market.MarketDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final MarketDataService marketDataService;
    private final PortfolioMapper portfolioMapper;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            MarketDataService marketDataService,
                            PortfolioMapper portfolioMapper) {
        this.watchlistRepository = watchlistRepository;
        this.marketDataService = marketDataService;
        this.portfolioMapper = portfolioMapper;
    }

    @Transactional
    public WatchlistDto addWatchlist(Long userId, String symbol, String sectorCode) {
        String resolvedSymbol = normalizeSymbol(symbol);
        String resolvedSectorCode = normalizeSectorCode(sectorCode);

        WatchlistItem item = watchlistRepository.findByUserIdAndSymbol(userId, resolvedSymbol)
                .map(existing -> {
                    existing.setSectorCode(resolvedSectorCode);
                    return watchlistRepository.save(existing);
                })
                .orElseGet(() -> watchlistRepository.save(new WatchlistItem(userId, resolvedSymbol, resolvedSectorCode)));

        LatestQuote latestQuote = marketDataService.getLatestQuote(resolvedSymbol);
        return portfolioMapper.toWatchlistDto(item, latestQuote);
    }

    @Transactional
    public void removeWatchlist(Long userId, String symbol) {
        String resolvedSymbol = normalizeSymbol(symbol);
        if (!watchlistRepository.existsByUserIdAndSymbol(userId, resolvedSymbol)) {
            throw new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "watchlist item not found for symbol " + resolvedSymbol);
        }
        watchlistRepository.deleteByUserIdAndSymbol(userId, resolvedSymbol);
    }

    public List<WatchlistDto> getWatchlist(Long userId) {
        return watchlistRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(WatchlistItem::getSymbol))
                .map(item -> portfolioMapper.toWatchlistDto(item, marketDataService.getLatestQuote(item.getSymbol())))
                .toList();
    }

    private String normalizeSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "symbol must not be blank");
        }
        return symbol.trim().toUpperCase();
    }

    private String normalizeSectorCode(String sectorCode) {
        if (!StringUtils.hasText(sectorCode)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "sectorCode must not be blank");
        }
        return sectorCode.trim().toUpperCase();
    }
}
