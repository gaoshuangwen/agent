package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.Map;
import java.util.Objects;

public final class LLMNode extends AbstractNode {
    
    private final ChatLanguageModel model;
    private final PromptTemplate promptTemplate;
    private final String outputKey;
    private final LLMConfig config;

    public LLMNode(NodeId id, String name, Map<String, Object> metadata,
                  ChatLanguageModel model, PromptTemplate promptTemplate,
                  String outputKey, LLMConfig config) {
        super(id, name, metadata);
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.promptTemplate = Objects.requireNonNull(promptTemplate, "Prompt template cannot be null");
        this.outputKey = Objects.requireNonNull(outputKey, "Output key cannot be null");
        this.config = config;
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) throws Exception {
        String prompt = promptTemplate.render(inputState.data());
        
        String response = model.generate(prompt);
        
        return inputState
                .with(outputKey, response)
                .with("__llm_prompt__", prompt)
                .with("__llm_model__", config != null ? config.modelName() : "unknown");
    }

    public static Builder builder(NodeId id, String name, ChatLanguageModel model) {
        return new Builder(id, name, model);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final ChatLanguageModel model;
        private final Map<String, Object> metadata = new java.util.HashMap<>();
        private PromptTemplate promptTemplate;
        private String outputKey = "llm_response";
        private LLMConfig config;

        private Builder(NodeId id, String name, ChatLanguageModel model) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
            this.model = Objects.requireNonNull(model);
        }

        public Builder promptTemplate(PromptTemplate template) {
            this.promptTemplate = template;
            return this;
        }

        public Builder prompt(String template) {
            this.promptTemplate = PromptTemplate.of(template);
            return this;
        }

        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public Builder config(LLMConfig config) {
            this.config = config;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public LLMNode build() {
            if (promptTemplate == null) {
                throw new IllegalStateException("Prompt template must be set");
            }
            return new LLMNode(id, name, metadata, model, promptTemplate, outputKey, config);
        }
    }
}
