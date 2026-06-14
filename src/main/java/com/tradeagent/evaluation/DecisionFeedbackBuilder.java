package com.tradeagent.evaluation;

import com.tradeagent.evaluation.EvaluationModels.ScoreDetailDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class DecisionFeedbackBuilder {

    public String buildFeedback(BigDecimal entryScore,
                                BigDecimal exitScore,
                                BigDecimal riskScore,
                                BigDecimal diversificationScore,
                                BigDecimal sectorFitScore) {
        return String.join(" ",
                messageFor("진입 타이밍", entryScore, true),
                messageFor("청산 타이밍", exitScore, true),
                messageFor("리스크 관리", riskScore, false),
                "분산 투자와 섹터 적합성은 참고 지표로만 반영됩니다."
        );
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
                new ScoreDetailDto("total", scale(totalScore), totalMessage(totalScore))
        );
    }

    public String determineMainWeakness(BigDecimal averageEntryScore,
                                        BigDecimal averageExitScore,
                                        BigDecimal averageRiskScore,
                                        BigDecimal averageDiversificationScore,
                                        BigDecimal averageSectorFitScore) {
        BigDecimal entry = scale(averageEntryScore);
        BigDecimal exit = scale(averageExitScore);
        BigDecimal risk = scale(averageRiskScore);

        if (entry.compareTo(exit) <= 0 && entry.compareTo(risk) <= 0) {
            return "진입 타이밍 개선 필요";
        }

        if (exit.compareTo(entry) <= 0 && exit.compareTo(risk) <= 0) {
            return "청산 타이밍 개선 필요";
        }

        return "리스크 관리 개선 필요";
    }

    private String messageFor(String label, BigDecimal score, boolean timing) {
        BigDecimal safeScore = scale(score);

        if (safeScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return label + (timing ? "은 우수합니다." : "가 안정적입니다.");
        }

        if (safeScore.compareTo(BigDecimal.valueOf(65)) >= 0) {
            return label + (timing ? "은 양호합니다." : "가 양호합니다.");
        }

        if (safeScore.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return label + (timing ? "은 보통 수준입니다." : "는 보통 수준입니다.");
        }

        return label + (timing ? " 개선이 필요합니다." : "가 아쉽습니다.");
    }

    private String totalMessage(BigDecimal totalScore) {
        BigDecimal safeScore = scale(totalScore);

        if (safeScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "전반적인 투자 판단 품질이 우수합니다.";
        }

        if (safeScore.compareTo(BigDecimal.valueOf(65)) >= 0) {
            return "전반적인 투자 판단 품질이 양호합니다.";
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
}