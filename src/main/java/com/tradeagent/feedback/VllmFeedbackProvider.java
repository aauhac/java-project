package com.tradeagent.feedback;

import org.springframework.stereotype.Component;

@Component
public class VllmFeedbackProvider implements FeedbackProvider {

    private final VllmClient vllmClient;
    private final TemplateFeedbackProvider templateFeedbackProvider;

    public VllmFeedbackProvider(VllmClient vllmClient, TemplateFeedbackProvider templateFeedbackProvider) {
        this.vllmClient = vllmClient;
        this.templateFeedbackProvider = templateFeedbackProvider;
    }

    @Override
    public String generate(String input) {
        String generated = vllmClient.generateFeedback(input);
        if ("vLLM 서버 연동 전 기본 피드백입니다.".equals(generated)) {
            return templateFeedbackProvider.generate(input);
        }
        return generated;
    }
}
