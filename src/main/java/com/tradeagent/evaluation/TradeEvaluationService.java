package com.tradeagent.evaluation;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.TradeType;
import com.tradeagent.common.ValidationException;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.PortfolioRepository;
import com.tradeagent.portfolio.TradeHistory;
import com.tradeagent.portfolio.TradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TradeEvaluationService {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final PriceBarRepository priceBarRepository;
    private final TradeEvaluationRepository tradeEvaluationRepository;
    private final EntryScoreCalculator entryScoreCalculator;
    private final ExitScoreCalculator exitScoreCalculator;
    private final RiskScoreCalculator riskScoreCalculator;
    private final DiversificationScoreCalculator diversificationScoreCalculator;
    private final SectorFitScoreCalculator sectorFitScoreCalculator;
    private final TotalDecisionScoreCalculator totalDecisionScoreCalculator;
    private final DecisionFeedbackBuilder decisionFeedbackBuilder;

    public TradeEvaluationService(TradeHistoryRepository tradeHistoryRepository,
                                  PortfolioRepository portfolioRepository,
                                  PriceBarRepository priceBarRepository,
                                  TradeEvaluationRepository tradeEvaluationRepository,
                                  EntryScoreCalculator entryScoreCalculator,
                                  ExitScoreCalculator exitScoreCalculator,
                                  RiskScoreCalculator riskScoreCalculator,
                                  DiversificationScoreCalculator diversificationScoreCalculator,
                                  SectorFitScoreCalculator sectorFitScoreCalculator,
                                  TotalDecisionScoreCalculator totalDecisionScoreCalculator,
                                  DecisionFeedbackBuilder decisionFeedbackBuilder) {
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.portfolioRepository = portfolioRepository;
        this.priceBarRepository = priceBarRepository;
        this.tradeEvaluationRepository = tradeEvaluationRepository;
        this.entryScoreCalculator = entryScoreCalculator;
        this.exitScoreCalculator = exitScoreCalculator;
        this.riskScoreCalculator = riskScoreCalculator;
        this.diversificationScoreCalculator = diversificationScoreCalculator;
        this.sectorFitScoreCalculator = sectorFitScoreCalculator;
        this.totalDecisionScoreCalculator = totalDecisionScoreCalculator;
        this.decisionFeedbackBuilder = decisionFeedbackBuilder;
    }

    @Transactional
    public TradeEvaluationDto evaluateTrade(Long tradeHistoryId) {
        long resolvedTradeHistoryId = validateTradeId(tradeHistoryId);
        TradeHistory tradeHistory = tradeHistoryRepository.findById(resolvedTradeHistoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND,
                        "trade history not found for id " + resolvedTradeHistoryId));

        return evaluateTradeInternal(tradeHistory);
    }

    @Transactional
    public List<TradeEvaluationDto> evaluateAllTrades(Long userId) {
        long resolvedUserId = validateUserId(userId);
        return tradeHistoryRepository.findByUserId(resolvedUserId).stream()
                .sorted(Comparator.comparing(TradeHistory::getTradedAt))
                .map(this::evaluateTradeInternal)
                .toList();
    }

    @Transactional
    public DecisionSummaryDto getDecisionSummary(Long userId) {
        List<TradeEvaluationDto> evaluations = evaluateAllTrades(userId);
        if (evaluations.isEmpty()) {
            return new DecisionSummaryDto(
                    zeroScore(),
                    zeroScore(),
                    zeroScore(),
                    zeroScore(),
                    zeroScore(),
                    zeroScore(),
                    "평가할 거래가 없습니다."
            );
        }

        BigDecimal averageEntry = average(evaluations.stream().map(TradeEvaluationDto::entryScore).toList());
        BigDecimal averageExit = average(evaluations.stream().map(TradeEvaluationDto::exitScore).toList());
        BigDecimal averageRisk = average(evaluations.stream().map(TradeEvaluationDto::riskScore).toList());
        BigDecimal averageDiversification = average(evaluations.stream().map(TradeEvaluationDto::diversificationScore).toList());
        BigDecimal averageSectorFit = average(evaluations.stream().map(TradeEvaluationDto::sectorFitScore).toList());
        BigDecimal averageTotal = average(evaluations.stream().map(TradeEvaluationDto::totalScore).toList());

        return new DecisionSummaryDto(
                averageEntry,
                averageExit,
                averageRisk,
                averageDiversification,
                averageSectorFit,
                averageTotal,
                decisionFeedbackBuilder.determineMainWeakness(
                        averageEntry,
                        averageExit,
                        averageRisk,
                        averageDiversification,
                        averageSectorFit
                )
        );
    }

    private TradeEvaluationDto evaluateTradeInternal(TradeHistory tradeHistory) {
        TradePairContext tradePairContext = resolveTradePair(tradeHistory);
        List<PortfolioPosition> positions = portfolioRepository.findByUserId(tradeHistory.getUserId());

        BigDecimal entryScore = entryScoreCalculator.calculate(new EntryScoreInput(
                tradePairContext.buyTrade(),
                loadBarsAfterEntry(tradePairContext.buyTrade())
        ));
        BigDecimal exitScore = exitScoreCalculator.calculate(new ExitScoreInput(
                tradePairContext.sellTrade(),
                loadBarsAroundExit(tradePairContext.sellTrade())
        ));
        BigDecimal riskScore = riskScoreCalculator.calculate(new RiskScoreInput(
                tradePairContext.buyTrade(),
                tradePairContext.sellTrade(),
                loadHoldingBars(tradePairContext.buyTrade(), tradePairContext.sellTrade())
        ));
        BigDecimal diversificationScore = diversificationScoreCalculator.calculate(new DiversificationScoreInput(
                tradeHistory.getUserId(),
                positions
        ));
        BigDecimal sectorFitScore = sectorFitScoreCalculator.calculate(new SectorFitScoreInput(
                tradeHistory.getSymbol(),
                tradeHistory.getSectorCode(),
                tradeHistory.getTradedAt().toLocalDate(),
                null
        ));
        BigDecimal totalScore = totalDecisionScoreCalculator.calculate(
                entryScore,
                exitScore,
                riskScore,
                diversificationScore,
                sectorFitScore
        );

        TradeEvaluation persisted = tradeEvaluationRepository.findByTradeHistoryId(tradeHistory.getId())
                .map(existing -> {
                    existing.updateScores(
                            entryScore,
                            exitScore,
                            riskScore,
                            diversificationScore,
                            sectorFitScore,
                            totalScore,
                            DateTimeUtil.nowUtc()
                    );
                    return tradeEvaluationRepository.save(existing);
                })
                .orElseGet(() -> tradeEvaluationRepository.save(new TradeEvaluation(
                        tradeHistory.getId(),
                        entryScore,
                        exitScore,
                        riskScore,
                        diversificationScore,
                        sectorFitScore,
                        totalScore,
                        DateTimeUtil.nowUtc()
                )));

        String feedback = decisionFeedbackBuilder.buildFeedback(
                persisted.getEntryScore(),
                persisted.getExitScore(),
                persisted.getRiskScore(),
                persisted.getDiversificationScore(),
                persisted.getSectorFitScore()
        );

        return new TradeEvaluationDto(
                persisted.getTradeHistoryId(),
                persisted.getEntryScore(),
                persisted.getExitScore(),
                persisted.getRiskScore(),
                persisted.getDiversificationScore(),
                persisted.getSectorFitScore(),
                persisted.getTotalScore(),
                feedback,
                persisted.getEvaluatedAt(),
                decisionFeedbackBuilder.buildScoreDetails(
                        persisted.getEntryScore(),
                        persisted.getExitScore(),
                        persisted.getRiskScore(),
                        persisted.getDiversificationScore(),
                        persisted.getSectorFitScore(),
                        persisted.getTotalScore()
                )
        );
    }

    private TradePairContext resolveTradePair(TradeHistory anchorTrade) {
        List<TradeHistory> trades = tradeHistoryRepository.findByUserIdAndSymbol(anchorTrade.getUserId(), anchorTrade.getSymbol()).stream()
                .sorted(Comparator.comparing(TradeHistory::getTradedAt))
                .toList();

        TradeHistory buyTrade = null;
        TradeHistory sellTrade = null;

        if (anchorTrade.getTradeType() == TradeType.BUY) {
            buyTrade = anchorTrade;
            sellTrade = trades.stream()
                    .filter(trade -> trade.getTradeType() == TradeType.SELL)
                    .filter(trade -> !trade.getTradedAt().isBefore(anchorTrade.getTradedAt()))
                    .findFirst()
                    .orElse(null);
        } else if (anchorTrade.getTradeType() == TradeType.SELL) {
            sellTrade = anchorTrade;
            buyTrade = trades.stream()
                    .filter(trade -> trade.getTradeType() == TradeType.BUY)
                    .filter(trade -> !trade.getTradedAt().isAfter(anchorTrade.getTradedAt()))
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        return new TradePairContext(buyTrade, sellTrade);
    }

    private List<PriceBar> loadBarsAfterEntry(TradeHistory buyTrade) {
        if (buyTrade == null) {
            return List.of();
        }

        LocalDate tradeDate = buyTrade.getTradedAt().toLocalDate();
        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        buyTrade.getSymbol(),
                        tradeDate.atStartOfDay(),
                        tradeDate.plusDays(10).atTime(LocalTime.MAX))
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .limit(5)
                .toList();
    }

    private List<PriceBar> loadBarsAroundExit(TradeHistory sellTrade) {
        if (sellTrade == null) {
            return List.of();
        }

        LocalDate tradeDate = sellTrade.getTradedAt().toLocalDate();
        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        sellTrade.getSymbol(),
                        tradeDate.minusDays(5).atStartOfDay(),
                        tradeDate.plusDays(5).atTime(LocalTime.MAX))
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .toList();
    }

    private List<PriceBar> loadHoldingBars(TradeHistory buyTrade, TradeHistory sellTrade) {
        if (buyTrade == null) {
            return List.of();
        }

        LocalDate startDate = buyTrade.getTradedAt().toLocalDate();
        LocalDate endDate = sellTrade != null
                ? sellTrade.getTradedAt().toLocalDate()
                : DateTimeUtil.today();

        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }

        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        buyTrade.getSymbol(),
                        startDate.atStartOfDay(),
                        endDate.atTime(LocalTime.MAX))
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .toList();
    }

    private long validateTradeId(Long tradeHistoryId) {
        if (tradeHistoryId == null || tradeHistoryId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "tradeHistoryId must be a positive number");
        }
        return tradeHistoryId;
    }

    private long validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
        return userId;
    }

    private BigDecimal average(List<BigDecimal> scores) {
        if (scores.isEmpty()) {
            return zeroScore();
        }

        BigDecimal total = scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroScore() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private record TradePairContext(TradeHistory buyTrade, TradeHistory sellTrade) {
    }
}
