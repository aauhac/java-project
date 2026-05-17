package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.ScoreDetailDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class DecisionFeedbackBuilder {

    public String buildFeedback(BigDecimal entryScore,
                                BigDecimal exitScore,
                                BigDecimal riskScore,
                                BigDecimal diversificationScore,
                                BigDecimal sectorFitScore) {
        List<String> parts = new ArrayList<>();
        parts.add(messageFor("진입 타이밍", entryScore, true));
        parts.add(messageFor("청산 타이밍", exitScore, true));
        parts.add(messageFor("리스크 관리", riskScore, false));
        parts.add(messageFor("분산 투자", diversificationScore, false));
        if (sectorFitScore == null || sectorFitScore.compareTo(BigDecimal.valueOf(50)) == 0) {
            parts.add("섹터 적합성 평가는 현재 기본값으로 계산되었습니다.");
        } else {
            parts.add(messageFor("섹터 적합성", sectorFitScore, false));
        }
        return String.join(" ", parts);
    }

    public List<ScoreDetailDto> buildScoreDetails(BigDecimal entryScore,
                                                  BigDecimal exitScore,
                                                  BigDecimal riskScore,
                                                  BigDecimal diversificationScore,
                                                  BigDecimal sectorFitScore,
                                                  BigDecimal totalScore) {
        return List.of(
                new ScoreDetailDto("entry", scale(entryScore), messageFor("진입 타이밍", entryScore, true)),
                new ScoreDetailDto("exit", scale(exitScore), messageFor("청산 타이밍", exitScore, true)),
                new ScoreDetailDto("risk", scale(riskScore), messageFor("리스크 관리", riskScore, false)),
                new ScoreDetailDto("diversification", scale(diversificationScore), messageFor("분산 투자", diversificationScore, false)),
                new ScoreDetailDto("sectorFit", scale(sectorFitScore),
                        sectorFitScore == null || sectorFitScore.compareTo(BigDecimal.valueOf(50)) == 0
                                ? "섹터 데이터가 아직 연결되지 않아 기본값으로 계산했습니다."
                                : messageFor("섹터 적합성", sectorFitScore, false)),
                new ScoreDetailDto("total", scale(totalScore), totalMessage(totalScore))
        );
    }

    public String determineMainWeakness(BigDecimal averageEntryScore,
                                        BigDecimal averageExitScore,
                                        BigDecimal averageRiskScore,
                                        BigDecimal averageDiversificationScore,
                                        BigDecimal averageSectorFitScore) {
        List<Metric> metrics = List.of(
                new Metric("진입 타이밍 개선 필요", scale(averageEntryScore)),
                new Metric("청산 타이밍 개선 필요", scale(averageExitScore)),
                new Metric("리스크 관리 개선 필요", scale(averageRiskScore)),
                new Metric("분산 투자 개선 필요", scale(averageDiversificationScore)),
                new Metric("섹터 적합성 데이터 보강 필요", scale(averageSectorFitScore))
        );

        return metrics.stream()
                .min((left, right) -> left.score().compareTo(right.score()))
                .map(Metric::message)
                .orElse("평가 데이터가 충분하지 않습니다.");
    }

    private String messageFor(String label, BigDecimal score, boolean timing) {
        BigDecimal safeScore = scale(score);
        if (safeScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return label + (timing ? "은 양호합니다." : "가 안정적입니다.");
        }
        if (safeScore.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return label + (timing ? "은 보통 수준입니다." : "는 보통 수준입니다.");
        }
        return label + (timing ? " 개선이 필요합니다." : "가 아쉽습니다.");
    }

    private String totalMessage(BigDecimal totalScore) {
        BigDecimal safeScore = scale(totalScore);
        if (safeScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "전반적인 투자 판단 품질이 우수합니다.";
        }
        if (safeScore.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "전반적인 투자 판단 품질은 보통 수준입니다.";
        }
        return "전반적인 투자 판단 품질 개선이 필요합니다.";
    }

    private BigDecimal scale(BigDecimal score) {
        if (score == null) {
            return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private record Metric(String message, BigDecimal score) {
    }
}
