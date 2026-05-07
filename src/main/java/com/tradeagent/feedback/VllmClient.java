package com.tradeagent.feedback;

import com.tradeagent.config.VllmProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class VllmClient {

    private static final String FALLBACK_MESSAGE = "vLLM 서버 연동 전 기본 피드백입니다.";

    private final WebClient webClient;
    private final VllmProperties vllmProperties;

    public VllmClient(WebClient.Builder webClientBuilder, VllmProperties vllmProperties) {
        this.vllmProperties = vllmProperties;
        this.webClient = webClientBuilder.baseUrl(normalizeBaseUrl(vllmProperties.getBaseUrl())).build();
    }

    public String generateFeedback(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return FALLBACK_MESSAGE;
        }

        try {
            VllmChatResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(new VllmChatRequest(
                            vllmProperties.getModel(),
                            List.of(new VllmMessage("user", prompt)),
                            vllmProperties.getTemperature(),
                            vllmProperties.getMaxTokens()
                    ))
                    .retrieve()
                    .bodyToMono(VllmChatResponse.class)
                    .block(Duration.ofSeconds(vllmProperties.getTimeoutSeconds()));

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return FALLBACK_MESSAGE;
            }

            VllmChoice firstChoice = response.choices().get(0);
            if (firstChoice == null || firstChoice.message() == null || !StringUtils.hasText(firstChoice.message().content())) {
                return FALLBACK_MESSAGE;
            }

            return firstChoice.message().content().trim();
        } catch (Exception ex) {
            return FALLBACK_MESSAGE;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:8000";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}

record VllmChatRequest(
        String model,
        List<VllmMessage> messages,
        double temperature,
        int max_tokens
) {
}

record VllmMessage(
        String role,
        String content
) {
}

record VllmChatResponse(
        List<VllmChoice> choices
) {
}

record VllmChoice(
        VllmMessage message
) {
}
