package com.festiva.ai;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@SuppressWarnings("unused")
class AiIcsNameExtractorConfig {

    @Bean
    OpenAiChatModel openAiChatModel(
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.model-name}") String modelName) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.0)
                .maxCompletionTokens(64)
                .timeout(Duration.ofSeconds(2))
                .maxRetries(0)
                .build();
    }

    @Bean
    IcsNameExtractorService icsNameExtractorService(OpenAiChatModel model, AiUsageBudget budget) {
        var delegate = AiServices.builder(IcsNameExtractorService.class)
                .chatModel(model)
                .build();
        return new BudgetedNameExtractor(delegate, budget);
    }
}
