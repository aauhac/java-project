package com.tradeagent.evaluation;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.TradeType;
import com.tradeagent.common.ValidationException;
import com.tradeagent.evaluation.EvaluationModels.DecisionSummaryDto;
import com.tradeagent.evaluation.EvaluationModels.TradeEvaluationDto;
import com.tradeagent.market.PriceBar;
import com.tradeagent.market.PriceBarRepository;
import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.PortfolioRepository;
import com.tradeagent.portfolio.TradeHistory;
import com.tradeagent.portfolio.TradeHistoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TradeEvaluationService {

    private static final int ENTRY_LOOKAHEAD_DAYS = 10;
    private static final int ENTRY_BAR_LIMIT = 8;
    private static final int EXIT_WINDOW_DAYS = 7;

    private final TradeHistoryRepository tradeHistoryRepository;
    private final PortfolioRepository portfolioRepository;
    private final PriceBarRepository priceBarRepository;
    private final TradeEvaluationRepository tradeEvaluationRepository;
    private final DecisionFeedbackBuilder decisionFeedbackBuilder;

    private final Object evaluationLock = new Object();

    public TradeEvaluationService(TradeHistoryRepository tradeHistoryRepository,
                                  PortfolioRepository portfolioRepository,
                                  PriceBarRepository priceBarRepository,
                                  TradeEvaluationRepository tradeEvaluationRepository,
                                  DecisionFeedbackBuilder decisionFeedbackBuilder) {
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.portfolioRepository = portfolioRepository;
        this.priceBarRepository = priceBarRepository;
        this.tradeEvaluationRepository = tradeEvaluationRepository;
        this.decisionFeedbackBuilder = decisionFeedbackBuilder;
    }

    @Transactional
    public TradeEvaluationDto evaluateTrade(Long tradeHistoryId) {
        synchronized (evaluationLock) {
            TradeHistory tradeHistory = tradeHistoryRepository.findById(tradeHistoryId)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.TRADE_NOT_FOUND,
                            "trade history not found for id " + tradeHistoryId
                    ));

            return evaluateTradeInternal(tradeHistory);
        }
    }

    @Transactional
    public List<TradeEvaluationDto> evaluateAllTrades(Long userId) {
        validateUserId(userId);

        synchronized (evaluationLock) {
            return tradeHistoryRepository.findByUserId(userId).stream()
                    .sorted(Comparator.comparing(TradeHistory::getTradedAt))
                    .map(this::evaluateTradeInternal)
                    .toList();
        }
    }

    /*
     * 중요:
     * 요약 조회에서는 DB에 update/save를 하지 않는다.
     * 화면에서 점수를 보여줄 때마다 trade_evaluations를 갱신하면 H2 lock timeout이 발생한다.
     * 따라서 요약은 현재 거래 이력과 가격 데이터로 메모리상 계산만 수행한다.
     */
    public DecisionSummaryDto getDecisionSummary(Long userId) {
        validateUserId(userId);

        List<TradeEvaluationDto> evaluations = tradeHistoryRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(TradeHistory::getTradedAt))
                .map(this::evaluateTradeInMemory)
                .toList();

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

        return buildSummary(evaluations);
    }

    private DecisionSummaryDto buildSummary(List<TradeEvaluationDto> evaluations) {
        BigDecimal averageEntry = averageOf(evaluations, TradeEvaluationDto::entryScore);
        BigDecimal averageExit = averageOf(evaluations, TradeEvaluationDto::exitScore);
        BigDecimal averageRisk = averageOf(evaluations, TradeEvaluationDto::riskScore);
        BigDecimal averageDiversification = averageOf(evaluations, TradeEvaluationDto::diversificationScore);
        BigDecimal averageSectorFit = averageOf(evaluations, TradeEvaluationDto::sectorFitScore);

        /*
         * 종합 판단은 실제 매매 판단 품질인 Entry / Exit / Risk 중심으로 계산한다.
         * 분산 투자와 섹터 적합성은 참고값으로만 유지한다.
         */
        BigDecimal averageTotal = calculateCoreAverageTotal(averageEntry, averageExit, averageRisk);

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

    private BigDecimal calculateCoreAverageTotal(BigDecimal entry, BigDecimal exit, BigDecimal risk) {
        BigDecimal safeEntry = scale(entry);
        BigDecimal safeExit = scale(exit);
        BigDecimal safeRisk = scale(risk);

        return safeEntry.multiply(BigDecimal.valueOf(0.35))
                .add(safeExit.multiply(BigDecimal.valueOf(0.30)))
                .add(safeRisk.multiply(BigDecimal.valueOf(0.35)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageOf(
            List<TradeEvaluationDto> evaluations,
            Function<TradeEvaluationDto, BigDecimal> extractor
    ) {
        return average(evaluations.stream().map(extractor).toList());
    }

    private TradeEvaluationDto evaluateTradeInMemory(TradeHistory tradeHistory) {
        EvaluationScores scores = calculateScores(tradeHistory);

        BigDecimal entryScore = toScore(scores.entry());
        BigDecimal exitScore = toScore(scores.exit());
        BigDecimal riskScore = toScore(scores.risk());
        BigDecimal diversificationScore = toScore(scores.diversification());
        BigDecimal sectorFitScore = toScore(scores.sectorFit());
        BigDecimal totalScore = toScore(scores.total());

        String feedback = decisionFeedbackBuilder.buildFeedback(
                entryScore,
                exitScore,
                riskScore,
                diversificationScore,
                sectorFitScore
        );

        return new TradeEvaluationDto(
                tradeHistory.getId(),
                entryScore,
                exitScore,
                riskScore,
                diversificationScore,
                sectorFitScore,
                totalScore,
                feedback,
                DateTimeUtil.nowUtc(),
                decisionFeedbackBuilder.buildScoreDetails(
                        entryScore,
                        exitScore,
                        riskScore,
                        diversificationScore,
                        sectorFitScore,
                        totalScore
                )
        );
    }

    private TradeEvaluationDto evaluateTradeInternal(TradeHistory tradeHistory) {
        EvaluationScores scores = calculateScores(tradeHistory);
        TradeEvaluation persisted = upsertEvaluation(tradeHistory, scores);
        return toDto(persisted);
    }

    private EvaluationScores calculateScores(TradeHistory tradeHistory) {
        TradePairContext tradePairContext = resolveTradePair(tradeHistory);

        List<PortfolioPosition> positions = portfolioRepository.findByUserId(tradeHistory.getUserId());
        List<PriceBar> barsAfterEntry = loadBarsAfterEntry(tradePairContext.buyTrade());
        List<PriceBar> barsAroundExit = loadBarsAroundExit(tradePairContext.sellTrade());
        List<PriceBar> holdingBars = loadHoldingBars(tradePairContext.buyTrade(), tradePairContext.sellTrade());

        double entryScore = calculateEntryScore(tradePairContext.buyTrade(), barsAfterEntry);
        double exitScore = calculateExitScore(tradePairContext.sellTrade(), barsAroundExit);
        double riskScore = calculateRiskScore(tradePairContext.buyTrade(), holdingBars);
        double diversificationScore = calculateDiversificationScore(positions);
        double sectorFitScore = calculateSectorFitScore(tradeHistory.getSectorCode());
        double totalScore = calculateTotalScore(entryScore, exitScore, riskScore);

        return new EvaluationScores(entryScore, exitScore, riskScore, diversificationScore, sectorFitScore, totalScore);
    }

    private TradeEvaluation upsertEvaluation(TradeHistory tradeHistory, EvaluationScores scores) {
        return tradeEvaluationRepository.findByTradeHistoryId(tradeHistory.getId())
                .map(existing -> updateAndSave(existing, scores))
                .orElseGet(() -> createOrRecover(tradeHistory, scores));
    }

    private TradeEvaluation createOrRecover(TradeHistory tradeHistory, EvaluationScores scores) {
        try {
            return tradeEvaluationRepository.save(new TradeEvaluation(
                    tradeHistory.getId(),
                    toScore(scores.entry()),
                    toScore(scores.exit()),
                    toScore(scores.risk()),
                    toScore(scores.diversification()),
                    toScore(scores.sectorFit()),
                    toScore(scores.total()),
                    DateTimeUtil.nowUtc()
            ));
        } catch (DataIntegrityViolationException e) {
            TradeEvaluation existing = tradeEvaluationRepository.findByTradeHistoryId(tradeHistory.getId())
                    .orElseThrow(() -> e);
            return updateAndSave(existing, scores);
        }
    }

    private TradeEvaluation updateAndSave(TradeEvaluation evaluation, EvaluationScores scores) {
        evaluation.updateScores(
                toScore(scores.entry()),
                toScore(scores.exit()),
                toScore(scores.risk()),
                toScore(scores.diversification()),
                toScore(scores.sectorFit()),
                toScore(scores.total()),
                DateTimeUtil.nowUtc()
        );

        return tradeEvaluationRepository.save(evaluation);
    }

    private TradeEvaluationDto toDto(TradeEvaluation persisted) {
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

    private List<PriceBar> loadBarsAfterEntry(TradeHistory buyTrade) {
        if (buyTrade == null) {
            return List.of();
        }

        LocalDate tradeDate = buyTrade.getTradedAt().toLocalDate();

        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        buyTrade.getSymbol(),
                        tradeDate.atStartOfDay(),
                        tradeDate.plusDays(ENTRY_LOOKAHEAD_DAYS).atTime(LocalTime.MAX)
                )
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .limit(ENTRY_BAR_LIMIT)
                .toList();
    }

    private List<PriceBar> loadBarsAroundExit(TradeHistory sellTrade) {
        if (sellTrade == null) {
            return List.of();
        }

        LocalDate tradeDate = sellTrade.getTradedAt().toLocalDate();

        return priceBarRepository.findBySymbolAndBarTimeBetween(
                        sellTrade.getSymbol(),
                        tradeDate.minusDays(EXIT_WINDOW_DAYS).atStartOfDay(),
                        tradeDate.plusDays(EXIT_WINDOW_DAYS).atTime(LocalTime.MAX)
                )
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
                        endDate.atTime(LocalTime.MAX)
                )
                .stream()
                .sorted(Comparator.comparing(PriceBar::getBarTime))
                .toList();
    }

    private TradePairContext resolveTradePair(TradeHistory anchorTrade) {
        List<TradeHistory> trades = tradeHistoryRepository
                .findByUserIdAndSymbol(anchorTrade.getUserId(), anchorTrade.getSymbol())
                .stream()
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

    private double calculateEntryScore(TradeHistory buyTrade, List<PriceBar> barsAfterEntry) {
        if (buyTrade == null || barsAfterEntry.isEmpty() || buyTrade.getPrice() == null) {
            return neutralScore();
        }

        BigDecimal entryPrice = buyTrade.getPrice();

        BigDecimal maxHigh = barsAfterEntry.stream()
                .map(PriceBar::getHighPrice)
                .filter(price -> price != null)
                .max(Comparator.naturalOrder())
                .orElse(entryPrice);

        BigDecimal minLow = barsAfterEntry.stream()
                .map(PriceBar::getLowPrice)
                .filter(price -> price != null)
                .min(Comparator.naturalOrder())
                .orElse(entryPrice);

        BigDecimal lastClose = barsAfterEntry.stream()
                .map(PriceBar::getClosePrice)
                .filter(price -> price != null)
                .reduce((first, second) -> second)
                .orElse(entryPrice);

        double upsidePct = Math.max(0.0, percentChange(entryPrice, maxHigh));
        double closePct = percentChange(entryPrice, lastClose);
        double adversePct = Math.max(0.0, -percentChange(entryPrice, minLow));

        /*
         * 매수 후 실제 종가 흐름이 좋으면 가점,
         * 잠재 상승 여력이 있었으면 약한 가점,
         * 매수 직후 큰 하락을 맞았으면 감점.
         */
        return clampScore(52.0 + closePct * 2.0 + upsidePct * 0.8 - adversePct * 1.4);
    }

    private double calculateExitScore(TradeHistory sellTrade, List<PriceBar> barsAroundExit) {
        if (sellTrade == null || barsAroundExit.isEmpty() || sellTrade.getPrice() == null) {
            return neutralScore();
        }

        BigDecimal sellPrice = sellTrade.getPrice();

        List<PriceBar> afterBars = barsAroundExit.stream()
                .filter(bar -> bar.getBarTime() != null && bar.getBarTime().isAfter(sellTrade.getTradedAt()))
                .toList();

        if (afterBars.isEmpty()) {
            return neutralScore();
        }

        BigDecimal maxAfter = afterBars.stream()
                .map(PriceBar::getHighPrice)
                .filter(price -> price != null)
                .max(Comparator.naturalOrder())
                .orElse(sellPrice);

        BigDecimal minAfter = afterBars.stream()
                .map(PriceBar::getLowPrice)
                .filter(price -> price != null)
                .min(Comparator.naturalOrder())
                .orElse(sellPrice);

        BigDecimal lastClose = afterBars.stream()
                .map(PriceBar::getClosePrice)
                .filter(price -> price != null)
                .reduce((first, second) -> second)
                .orElse(sellPrice);

        double missedUpsidePct = Math.max(0.0, percentChange(sellPrice, maxAfter));
        double avoidedDropPct = Math.max(0.0, -percentChange(sellPrice, minAfter));
        double postClosePct = percentChange(sellPrice, lastClose);

        /*
         * 매도 후 종가가 더 올랐으면 감점,
         * 매도 후 하락을 피했으면 가점.
         */
        return clampScore(55.0 - missedUpsidePct * 1.4 - Math.max(0.0, postClosePct) * 1.2 + avoidedDropPct * 1.6);
    }

    private double calculateRiskScore(TradeHistory buyTrade, List<PriceBar> holdingBars) {
        if (buyTrade == null || holdingBars.isEmpty() || buyTrade.getPrice() == null) {
            return neutralScore();
        }

        BigDecimal entryPrice = buyTrade.getPrice();
        BigDecimal peak = entryPrice;
        double maxDrawdownPct = 0.0;
        double finalReturnPct = 0.0;

        for (PriceBar bar : holdingBars) {
            BigDecimal high = bar.getHighPrice();
            BigDecimal low = bar.getLowPrice();
            BigDecimal close = bar.getClosePrice();

            if (high != null && high.compareTo(peak) > 0) {
                peak = high;
            }

            if (peak != null && low != null && peak.compareTo(BigDecimal.ZERO) > 0) {
                double drawdownPct = peak.subtract(low)
                        .divide(peak, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

                maxDrawdownPct = Math.max(maxDrawdownPct, drawdownPct);
            }

            if (close != null) {
                finalReturnPct = percentChange(entryPrice, close);
            }
        }

        return clampScore(70.0 + finalReturnPct * 0.5 - maxDrawdownPct * 2.0);
    }

    private double calculateDiversificationScore(List<PortfolioPosition> positions) {
        if (positions == null || positions.isEmpty()) {
            return neutralScore();
        }

        BigDecimal total = positions.stream()
                .map(PortfolioPosition::getTotalBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return neutralScore();
        }

        Set<String> uniqueSectors = positions.stream()
                .map(PortfolioPosition::getSectorCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());

        Map<String, BigDecimal> sectorWeights = positions.stream()
                .filter(position -> position.getSectorCode() != null)
                .collect(Collectors.groupingBy(
                        PortfolioPosition::getSectorCode,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                PortfolioPosition::getTotalBuyAmount,
                                BigDecimal::add
                        )
                ));

        double maxWeightPct = sectorWeights.values().stream()
                .map(weight -> weight.divide(total, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue())
                .max(Double::compareTo)
                .orElse(100.0);

        double sectorBonus = Math.min(uniqueSectors.size() * 10.0, 35.0);
        double concentrationPenalty = Math.max(0.0, (maxWeightPct - 45.0) * 0.7);

        return clampScore(55.0 + sectorBonus - concentrationPenalty);
    }

    private double calculateSectorFitScore(String sectorCode) {
        if (sectorCode == null || sectorCode.isBlank()) {
            return neutralScore();
        }

        String normalized = sectorCode.toUpperCase();

        if (com.tradeagent.sector.SectorConstants.SUPPORTED_SECTORS.contains(normalized)) {
            return 72.0;
        }

        return 48.0;
    }

    private double calculateTotalScore(double entryScore,
                                       double exitScore,
                                       double riskScore) {
        return clampScore(
                entryScore * 0.35
                        + exitScore * 0.30
                        + riskScore * 0.35
        );
    }

    private double percentChange(BigDecimal from, BigDecimal to) {
        if (from == null || to == null || from.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        return to.subtract(from)
                .divide(from, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
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

    private BigDecimal scale(BigDecimal score) {
        if (score == null) {
            return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private double neutralScore() {
        return 50.0;
    }

    private BigDecimal toScore(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
    }

    private double clampScore(double score) {
        if (!Double.isFinite(score)) {
            return neutralScore();
        }

        if (score < 0.0) {
            return 0.0;
        }

        if (score > 100.0) {
            return 100.0;
        }

        return Math.round(score * 100.0) / 100.0;
    }

    private record TradePairContext(TradeHistory buyTrade, TradeHistory sellTrade) {
    }

    private record EvaluationScores(
            double entry,
            double exit,
            double risk,
            double diversification,
            double sectorFit,
            double total
    ) {
    }
}