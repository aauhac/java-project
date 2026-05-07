package com.tradeagent.feedback;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class TemplateFeedbackProvider implements FeedbackProvider {

    @Override
    public String generate(String input) {
        if (input == null || input.isBlank()) {
            return "분석 결과가 충분하지 않아 기본 피드백을 제공합니다.";
        }

        PromptData promptData = PromptData.parse(input);
        return switch (promptData.type()) {
            case "TRADE" -> buildTradeFeedback(promptData);
            case "OPPORTUNITY" -> buildOpportunityFeedback(promptData);
            case "SECTOR" -> buildSectorFeedback(promptData);
            case "OVERALL" -> buildOverallFeedback(promptData);
            default -> input;
        };
    }

    private String buildTradeFeedback(PromptData promptData) {
        double entry = promptData.doubleValue("entry");
        double exit = promptData.doubleValue("exit");
        double risk = promptData.doubleValue("risk");
        double overall = promptData.doubleValue("overall");
        String weakness = promptData.value("weakness");

        StringBuilder message = new StringBuilder();
        message.append(overall >= 75
                ? "전반적으로 투자 판단이 양호합니다. "
                : overall >= 50
                ? "전반적인 투자 판단은 보통 수준입니다. "
                : "전반적인 투자 판단 개선이 필요합니다. ");

        message.append(entry >= 70
                ? "진입 타이밍은 비교적 안정적입니다. "
                : "진입 타이밍은 더 신중하게 다듬을 여지가 있습니다. ");
        message.append(exit >= 70
                ? "청산 타이밍도 비교적 잘 관리되었습니다. "
                : "청산 타이밍은 개선이 필요합니다. ");
        message.append(risk >= 60
                ? "리스크 관리도 무난한 편입니다. "
                : "리스크 관리 측면의 보완이 필요합니다. ");

        if (!weakness.isBlank()) {
            message.append("현재 가장 약한 부분은 ").append(weakness).append("입니다.");
        }
        return message.toString().trim();
    }

    private String buildOpportunityFeedback(PromptData promptData) {
        int missed = promptData.intValue("missed");
        int avoided = promptData.intValue("avoided");
        int soldEarly = promptData.intValue("soldEarly");
        int heldLong = promptData.intValue("heldLong");

        StringBuilder message = new StringBuilder();
        if (missed > 0) {
            message.append("관심 종목 중 놓친 기회가 ").append(missed).append("건 있습니다. ");
        } else {
            message.append("현재 뚜렷한 놓친 기회는 많지 않습니다. ");
        }
        if (avoided > 0) {
            message.append("피한 위험은 ").append(avoided).append("건으로 확인됩니다. ");
        } else {
            message.append("피한 위험 사례는 아직 많지 않습니다. ");
        }
        if (soldEarly > 0) {
            message.append("너무 빨리 판 거래가 ").append(soldEarly).append("건 있어 청산 전략 점검이 필요합니다. ");
        }
        if (heldLong > 0) {
            message.append("너무 오래 보유한 거래가 ").append(heldLong).append("건 있어 손절/정리 기준을 보완하는 것이 좋습니다.");
        }
        return message.toString().trim();
    }

    private String buildSectorFeedback(PromptData promptData) {
        double strongExposure = promptData.doubleValue("strongExposure");
        double weakExposure = promptData.doubleValue("weakExposure");
        String baseMessage = promptData.value("message");

        StringBuilder message = new StringBuilder();
        message.append(baseMessage.isBlank()
                ? "포트폴리오의 섹터 노출을 진단했습니다. "
                : baseMessage).append(" ");

        if (strongExposure > weakExposure) {
            message.append("강한 섹터 노출 비중이 ").append(formatPercent(strongExposure))
                    .append("로 약한 섹터보다 높습니다.");
        } else if (strongExposure < weakExposure) {
            message.append("약한 섹터 노출 비중이 ").append(formatPercent(weakExposure))
                    .append("로 더 높아 리밸런싱을 검토할 수 있습니다.");
        } else {
            message.append("강한 섹터와 약한 섹터 노출 비중이 비슷합니다.");
        }
        return message.toString().trim();
    }

    private String buildOverallFeedback(PromptData promptData) {
        double overallTradeScore = promptData.doubleValue("tradeScore");
        int missed = promptData.intValue("missed");
        double weakExposure = promptData.doubleValue("weakExposure");

        StringBuilder message = new StringBuilder();
        message.append(overallTradeScore >= 70
                ? "투자 판단의 기본기는 비교적 안정적입니다. "
                : "투자 판단의 기본기를 조금 더 다듬을 필요가 있습니다. ");

        if (missed > 0) {
            message.append("관심 종목 관리에서는 놓친 기회가 ").append(missed).append("건 보여 진입 타이밍 점검이 필요합니다. ");
        } else {
            message.append("관심 종목 관리에서는 큰 놓친 기회가 두드러지지 않습니다. ");
        }

        if (weakExposure >= 40) {
            message.append("또한 약한 섹터 노출이 높아 포트폴리오 재배치도 고려할 수 있습니다.");
        } else {
            message.append("섹터 노출도는 비교적 안정적인 편입니다.");
        }
        return message.toString().trim();
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private record PromptData(String type, Map<String, String> values) {

        static PromptData parse(String input) {
            String[] parts = input.split("\\|");
            String type = parts.length > 0 ? parts[0].trim().toUpperCase(Locale.ROOT) : "RAW";
            Map<String, String> values = new HashMap<>();
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                int separator = part.indexOf('=');
                if (separator > 0) {
                    values.put(part.substring(0, separator), part.substring(separator + 1));
                }
            }
            return new PromptData(type, values);
        }

        String value(String key) {
            return values.getOrDefault(key, "");
        }

        double doubleValue(String key) {
            try {
                return Double.parseDouble(value(key));
            } catch (Exception ex) {
                return 0.0d;
            }
        }

        int intValue(String key) {
            try {
                return Integer.parseInt(value(key));
            } catch (Exception ex) {
                return 0;
            }
        }
    }
}
