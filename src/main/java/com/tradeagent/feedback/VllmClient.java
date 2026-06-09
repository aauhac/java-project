package com.tradeagent.feedback;

import com.tradeagent.config.VllmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class VllmClient {

    private static final Logger log = LoggerFactory.getLogger(VllmClient.class);

    private static final String FALLBACK_MESSAGE = "vLLM 서버 연동 전 기본 피드백입니다.";
    private static final int MAX_TOKENS = 512;
    private static final double TEMPERATURE = 0.2;
    private static final int TIMEOUT_SECONDS = 120;

    private final WebClient webClient;
    private final VllmProperties vllmProperties;

    public VllmClient(WebClient.Builder webClientBuilder, VllmProperties vllmProperties) {
        this.vllmProperties = vllmProperties;
        this.webClient = webClientBuilder
                .baseUrl(normalizeBaseUrl(vllmProperties.getBaseUrl()))
                .build();
    }

    public String generateText(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return FALLBACK_MESSAGE;
        }

        if (!vllmProperties.isEnabled()) {
            log.warn("vLLM is disabled. Check VLLM_ENABLED.");
            return FALLBACK_MESSAGE;
        }

        try {
            WebClient.RequestBodySpec requestSpec = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Content-Type", "application/json");

            if (StringUtils.hasText(vllmProperties.getApiKey())) {
                requestSpec.header("Authorization", "Bearer " + vllmProperties.getApiKey());
            }

            VllmChatResponse response = requestSpec
                    .bodyValue(new VllmChatRequest(
                            vllmProperties.getModel(),
                            List.of(new VllmMessage("user", prompt)),
                            TEMPERATURE,
                            MAX_TOKENS
                    ))
                    .retrieve()
                    .bodyToMono(VllmChatResponse.class)
                    .block(Duration.ofSeconds(TIMEOUT_SECONDS));

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("vLLM response is empty.");
                return FALLBACK_MESSAGE;
            }

            VllmChoice firstChoice = response.choices().get(0);
            if (firstChoice == null
                    || firstChoice.message() == null
                    || !StringUtils.hasText(firstChoice.message().content())) {
                log.warn("vLLM response message is empty.");
                return FALLBACK_MESSAGE;
            }

            return firstChoice.message().content().trim();
        } catch (Exception ex) {
            log.warn("vLLM request failed. baseUrl={}, model={}, error={}",
                    vllmProperties.getBaseUrl(),
                    vllmProperties.getModel(),
                    ex.getMessage());
            return FALLBACK_MESSAGE;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:8000";
        }

        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }

        return normalized;
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