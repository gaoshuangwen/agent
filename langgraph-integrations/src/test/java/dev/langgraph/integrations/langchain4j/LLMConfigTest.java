package dev.langgraph.integrations.langchain4j;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class LLMConfigTest {

    @Test
    void shouldCreateBasicConfig() {
        LLMConfig config = LLMConfig.builder("gpt-4")
                .temperature(0.7)
                .maxTokens(1024)
                .build();

        assertEquals("gpt-4", config.modelName());
        assertEquals(0.7, config.temperature());
        assertEquals(1024, config.maxTokens());
    }

    @Test
    void shouldUseDefaultTimeout() {
        LLMConfig config = LLMConfig.builder("gpt-4").build();

        assertEquals(Duration.ofSeconds(60), config.timeout());
    }

    @Test
    void shouldSetCustomTimeout() {
        LLMConfig config = LLMConfig.builder("gpt-4")
                .timeout(Duration.ofSeconds(30))
                .build();

        assertEquals(Duration.ofSeconds(30), config.timeout());
    }

    @Test
    void shouldAddAdditionalParameters() {
        LLMConfig config = LLMConfig.builder("gpt-4")
                .parameter("top_p", 0.9)
                .parameter("presence_penalty", 0.1)
                .build();

        assertEquals(0.9, config.additionalParameters().get("top_p"));
        assertEquals(0.1, config.additionalParameters().get("presence_penalty"));
    }

    @Test
    void shouldValidateTemperature() {
        assertThrows(IllegalArgumentException.class, () -> {
            LLMConfig.builder("gpt-4").temperature(-0.1).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            LLMConfig.builder("gpt-4").temperature(2.1).build();
        });
    }

    @Test
    void shouldValidateMaxTokens() {
        assertThrows(IllegalArgumentException.class, () -> {
            LLMConfig.builder("gpt-4").maxTokens(0).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            LLMConfig.builder("gpt-4").maxTokens(-100).build();
        });
    }

    @Test
    void shouldCreateGPT4Config() {
        LLMConfig config = LLMConfig.gpt4();

        assertEquals("gpt-4", config.modelName());
        assertEquals(0.7, config.temperature());
        assertEquals(2048, config.maxTokens());
    }

    @Test
    void shouldCreateGPT35TurboConfig() {
        LLMConfig config = LLMConfig.gpt35Turbo();

        assertEquals("gpt-3.5-turbo", config.modelName());
        assertEquals(0.7, config.temperature());
        assertEquals(2048, config.maxTokens());
    }

    @Test
    void shouldCreateDefaultsConfig() {
        LLMConfig config = LLMConfig.defaults("claude-3");

        assertEquals("claude-3", config.modelName());
        assertEquals(0.7, config.temperature());
        assertEquals(2048, config.maxTokens());
    }

    @Test
    void shouldThrowOnNullModelName() {
        assertThrows(NullPointerException.class, () -> {
            LLMConfig.builder(null).build();
        });
    }
}
