package dev.langgraph.integrations.langchain4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LLMConfig {
    
    private final String modelName;
    private final Double temperature;
    private final Integer maxTokens;
    private final Duration timeout;
    private final Map<String, Object> additionalParameters;

    private LLMConfig(Builder builder) {
        this.modelName = Objects.requireNonNull(builder.modelName, "Model name cannot be null");
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.timeout = builder.timeout != null ? builder.timeout : Duration.ofSeconds(60);
        this.additionalParameters = Map.copyOf(builder.additionalParameters);
    }

    public static Builder builder(String modelName) {
        return new Builder(modelName);
    }

    public String modelName() {
        return modelName;
    }

    public Double temperature() {
        return temperature;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public Duration timeout() {
        return timeout;
    }

    public Map<String, Object> additionalParameters() {
        return additionalParameters;
    }

    public static final class Builder {
        private final String modelName;
        private Double temperature;
        private Integer maxTokens;
        private Duration timeout;
        private final Map<String, Object> additionalParameters = new HashMap<>();

        private Builder(String modelName) {
            this.modelName = Objects.requireNonNull(modelName);
        }

        public Builder temperature(double temperature) {
            if (temperature < 0.0 || temperature > 2.0) {
                throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0");
            }
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("Max tokens must be positive");
            }
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout);
            return this;
        }

        public Builder parameter(String key, Object value) {
            this.additionalParameters.put(key, value);
            return this;
        }

        public LLMConfig build() {
            return new LLMConfig(this);
        }
    }

    public static LLMConfig defaults(String modelName) {
        return builder(modelName)
                .temperature(0.7)
                .maxTokens(2048)
                .build();
    }

    public static LLMConfig gpt4() {
        return defaults("gpt-4");
    }

    public static LLMConfig gpt35Turbo() {
        return defaults("gpt-3.5-turbo");
    }
}
