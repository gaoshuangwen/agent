package dev.langgraph.integrations.langchain4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptTemplate {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");
    
    private final String template;
    private final Map<String, Object> defaultVariables;

    private PromptTemplate(String template, Map<String, Object> defaultVariables) {
        this.template = Objects.requireNonNull(template, "Template cannot be null");
        this.defaultVariables = Map.copyOf(defaultVariables);
    }

    public static PromptTemplate of(String template) {
        return new PromptTemplate(template, Map.of());
    }

    public static PromptTemplate of(String template, Map<String, Object> defaultVariables) {
        return new PromptTemplate(template, defaultVariables);
    }

    public String render(Map<String, Object> variables) {
        Map<String, Object> allVariables = new HashMap<>(defaultVariables);
        allVariables.putAll(variables);

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = allVariables.get(varName);
            
            if (value == null) {
                throw new IllegalArgumentException(
                        "Variable '" + varName + "' not found in variables");
            }
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public String render() {
        return render(Map.of());
    }

    public PromptTemplate withDefault(String key, Object value) {
        Map<String, Object> newDefaults = new HashMap<>(defaultVariables);
        newDefaults.put(key, value);
        return new PromptTemplate(template, newDefaults);
    }

    public String template() {
        return template;
    }

    public Map<String, Object> defaultVariables() {
        return defaultVariables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String template;
        private final Map<String, Object> defaultVariables = new HashMap<>();

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        public Builder defaultVariable(String key, Object value) {
            this.defaultVariables.put(key, value);
            return this;
        }

        public PromptTemplate build() {
            return new PromptTemplate(template, defaultVariables);
        }
    }

    public static PromptTemplate systemMessage(String content) {
        return of("System: " + content);
    }

    public static PromptTemplate userMessage(String content) {
        return of("User: " + content);
    }

    public static PromptTemplate assistantMessage(String content) {
        return of("Assistant: " + content);
    }
}
