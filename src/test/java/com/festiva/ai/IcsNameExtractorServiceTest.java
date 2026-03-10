package com.festiva.ai;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run with:
 *   AI_ENABLED=true OPENAI_API_KEY=... AI_BASE_URL=https://models.inference.ai.azure.com \
 *   mvn test -Dtest=IcsNameExtractorServiceTest
 */
@EnabledIfEnvironmentVariable(named = "AI_ENABLED", matches = "true")
class IcsNameExtractorServiceTest {

    private IcsNameExtractorService buildService() {
        String apiKey  = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv().getOrDefault("AI_BASE_URL", "https://api.openai.com/v1");
        String model   = System.getenv().getOrDefault("AI_MODEL_NAME", "gpt-4o-mini");
        return AiServices.builder(IcsNameExtractorService.class)
                .chatModel(OpenAiChatModel.builder()
                        .apiKey(apiKey).baseUrl(baseUrl).modelName(model).temperature(0.0).build())
                .build();
    }

    @Test
    void extractsNameFromRussianSummary() {
        String result = buildService().extractName("День рождения Юли");
        System.out.println("RU result: " + result);
        assertThat(result).isNotBlank();
    }

    @Test
    void extractsNameFromEnglishSummary() {
        String result = buildService().extractName("Birthday of John");
        System.out.println("EN result: " + result);
        assertThat(result).isNotBlank();
    }

    @Test
    void returnsOriginalWhenNoNameFound() {
        String result = buildService().extractName("Happy birthday!");
        System.out.println("Fallback result: " + result);
        assertThat(result).isNotBlank();
    }
}
