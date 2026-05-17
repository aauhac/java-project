package com.tradeagent.portfolio;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import com.tradeagent.market.LatestQuote;
import com.tradeagent.market.MarketDataService;
import com.tradeagent.portfolio.PortfolioApiModels.PortfolioSummaryDto;
import com.tradeagent.portfolio.PortfolioApiModels.PositionDto;
import com.tradeagent.portfolio.PortfolioApiModels.SectorAllocationDto;
import com.tradeagent.portfolio.PortfolioApiModels.TradeRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final TradeRegistrationService tradeRegistrationService;
    private final MarketDataService marketDataService;
    private final PortfolioMapper portfolioMapper;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            TradeRegistrationService tradeRegistrationService,
                            MarketDataService marketDataService,
                            PortfolioMapper portfolioMapper) {
        this.portfolioRepository = portfolioRepository;
        this.tradeRegistrationService = tradeRegistrationService;
        this.marketDataService = marketDataService;
        this.portfolioMapper = portfolioMapper;
    }

    @Transactional
    public PositionDto buyStock(TradeRequestDto request) {
        PortfolioPosition position = tradeRegistrationService.registerBuy(request);
        LatestQuote latestQuote = marketDataService.getLatestQuote(position.getSymbol());
        return portfolioMapper.toPositionDto(position, latestQuote);
    }

    @Transactional
    public void sellStock(TradeRequestDto request) {
        tradeRegistrationService.registerSell(request);
    }

    public List<PositionDto> getPositions(Long userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(PortfolioPosition::getSymbol))
                .map(position -> portfolioMapper.toPositionDto(position, marketDataService.getLatestQuote(position.getSymbol())))
                .toList();
    }

    public PortfolioSummaryDto getSummary(Long userId) {
        List<PortfolioPosition> positions = portfolioRepository.findByUserId(userId);
        BigDecimal totalBuyAmount = positions.stream()
                .map(PortfolioPosition::getTotalBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMarketValue = positions.stream()
                .map(position -> position.calculateMarketValue(marketDataService.getLatestQuote(position.getSymbol()).getLastPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return portfolioMapper.toSummaryDto(totalBuyAmount, totalMarketValue, positions.size());
    }

    public List<SectorAllocationDto> getSectorAllocation(Long userId) {
        List<PortfolioPosition> positions = portfolioRepository.findByUserId(userId);
        Map<String, BigDecimal> marketValueBySector = new LinkedHashMap<>();

        for (PortfolioPosition position : positions) {
            BigDecimal marketValue = position.calculateMarketValue(
                    marketDataService.getLatestQuote(position.getSymbol()).getLastPrice()
            );
            marketValueBySector.merge(position.getSectorCode(), marketValue, BigDecimal::add);
        }

        BigDecimal totalMarketValue = marketValueBySector.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return marketValueBySector.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> portfolioMapper.toSectorAllocationDto(entry.getKey(), entry.getValue(), totalMarketValue))
                .toList();
    }
}
