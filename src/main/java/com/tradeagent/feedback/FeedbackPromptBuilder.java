package com.tradeagent.feedback;

import com.tradeagent.evaluation.EvaluationModels.DecisionSummaryDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunitySummaryDto;
import com.tradeagent.sector.SectorApiModels.PortfolioSectorDiagnosticDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FeedbackPromptBuilder {

    public String buildTradeEvaluationPrompt(DecisionSummaryDto dto) {
        return String.format(Locale.ROOT,
                """
                너는 투자 기록을 설명하는 한국어 피드백 도우미다.
                반드시 한국어로만 답변하라.
                영어 문장, 영어 제목, markdown 구분선은 사용하지 마라.
                매수/매도 지시를 하지 말고, 사용자의 거래 습관과 위험 요인을 설명하라.
                답변은 5~7문장으로 작성하라.

                거래 평가 요약:
                - 진입 점수: %.2f점
                - 청산 점수: %.2f점
                - 리스크 관리 점수: %.2f점
                - 분산 투자 점수: %.2f점
                - 섹터 적합도 점수: %.2f점
                - 종합 점수: %.2f점
                - 주요 약점: %s

                답변 형식:
                1. 종합 점수를 먼저 해석한다.
                2. 가장 낮은 항목을 중심으로 문제를 설명한다.
                3. 사용자가 다음 거래에서 확인할 점을 제안한다.
                4. 투자 조언이 아니라 평가 설명이라는 점을 자연스럽게 유지한다.
                """,
                safe(dto.averageEntryScore()),
                safe(dto.averageExitScore()),
                safe(dto.averageRiskScore()),
                safe(dto.averageDiversificationScore()),
                safe(dto.averageSectorFitScore()),
                safe(dto.averageTotalScore()),
                sanitize(dto.mainWeakness()));
    }

    public String buildOpportunityPrompt(OpportunitySummaryDto dto) {
        return String.format(Locale.ROOT,
                """
                너는 투자 기회 분석 결과를 설명하는 한국어 피드백 도우미다.
                반드시 한국어로만 답변하라.
                영어 문장, 영어 제목, markdown 구분선은 사용하지 마라.
                매수/매도 지시를 하지 말고, 놓친 기회와 피한 손실을 설명하라.
                답변은 5~7문장으로 작성하라.

                기회 분석 요약:
                - 놓친 기회 수: %d건
                - 피한 손실 수: %d건
                - 너무 일찍 매도한 사례: %d건
                - 너무 오래 보유한 사례: %d건

                답변 형식:
                1. 놓친 기회와 피한 손실의 균형을 설명한다.
                2. 반복되는 매매 패턴이 있다면 언급한다.
                3. 다음 거래에서 확인할 점을 제안한다.
                """,
                dto.missedCount(),
                dto.avoidedCount(),
                dto.soldTooEarlyCount(),
                dto.heldTooLongCount());
    }

    public String buildSectorPrompt(PortfolioSectorDiagnosticDto dto) {
        return String.format(Locale.ROOT,
                """
                너는 포트폴리오의 섹터 노출을 설명하는 한국어 피드백 도우미다.
                반드시 한국어로만 답변하라.
                영어 문장, 영어 제목, markdown 구분선은 사용하지 마라.
                매수/매도 지시를 하지 말고, 섹터 집중도와 리스크를 설명하라.
                답변은 5~7문장으로 작성하라.

                섹터 진단 요약:
                - 강세 섹터 노출 비중: %.2f
                - 약세 섹터 노출 비중: %.2f
                - 진단 메시지: %s

                답변 형식:
                1. 현재 섹터 노출 상태를 설명한다.
                2. 약세 섹터 또는 과도한 집중이 있으면 리스크로 설명한다.
                3. 분산 관점에서 확인할 점을 제안한다.
                """,
                safe(dto.strongExposure()),
                safe(dto.weakExposure()),
                sanitize(dto.message()));
    }

    public String buildOverallPrompt(DecisionSummaryDto tradeSummary,
                                     OpportunitySummaryDto opportunitySummary,
                                     PortfolioSectorDiagnosticDto sectorDiagnostic) {
        return String.format(Locale.ROOT,
                """
                너는 사용자의 투자 활동을 종합적으로 설명하는 한국어 피드백 도우미다.
                반드시 한국어로만 답변하라.
                영어 문장, 영어 제목, markdown 구분선은 사용하지 마라.
                매수/매도 지시를 하지 말고, 거래 평가, 기회 분석, 섹터 노출을 종합해서 설명하라.
                답변은 6~9문장으로 작성하라.

                종합 데이터:
                - 거래 종합 점수: %.2f점
                - 놓친 기회 수: %d건
                - 피한 손실 수: %d건
                - 너무 일찍 매도한 사례: %d건
                - 너무 오래 보유한 사례: %d건
                - 강세 섹터 노출 비중: %.2f
                - 약세 섹터 노출 비중: %.2f

                답변 형식:
                1. 전체 투자 상태를 먼저 요약한다.
                2. 거래 습관, 기회 손실, 섹터 노출 중 가장 중요한 문제를 설명한다.
                3. 다음에 확인할 점을 2~3개 제안한다.
                4. 직접적인 매수/매도 지시는 하지 않는다.
                """,
                safe(tradeSummary.averageTotalScore()),
                opportunitySummary.missedCount(),
                opportunitySummary.avoidedCount(),
                opportunitySummary.soldTooEarlyCount(),
                opportunitySummary.heldTooLongCount(),
                safe(sectorDiagnostic.strongExposure()),
                safe(sectorDiagnostic.weakExposure()));
    }

    private double safe(java.math.BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', ' ').replace('=', ' ').trim();
    }
}