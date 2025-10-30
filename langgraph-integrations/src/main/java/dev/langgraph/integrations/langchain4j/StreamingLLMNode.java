package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class StreamingLLMNode extends AbstractNode {
    
    private final StreamingChatLanguageModel model;
    private final PromptTemplate promptTemplate;
    private final String outputKey;
    private final Consumer<String> tokenHandler;

    public StreamingLLMNode(NodeId id, String name, Map<String, Object> metadata,
                           StreamingChatLanguageModel model, PromptTemplate promptTemplate,
                           String outputKey, Consumer<String> tokenHandler) {
        super(id, name, metadata);
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.promptTemplate = Objects.requireNonNull(promptTemplate, "Prompt template cannot be null");
        this.outputKey = Objects.requireNonNull(outputKey, "Output key cannot be null");
        this.tokenHandler = tokenHandler;
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) throws Exception {
        String prompt = promptTemplate.render(inputState.data());
        
        StringBuilder fullResponse = new StringBuilder();
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        model.generate(prompt, new dev.langchain4j.model.StreamingResponseHandler<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                if (tokenHandler != null) {
                    tokenHandler.accept(token);
                }
            }

            @Override
            public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                future.complete(null);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });

        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Streaming failed", e);
        }

        return inputState
                .with(outputKey, fullResponse.toString())
                .with("__llm_prompt__", prompt)
                .with("__streaming__", true);
    }

    public static Builder builder(NodeId id, String name, StreamingChatLanguageModel model) {
        return new Builder(id, name, model);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final StreamingChatLanguageModel model;
        private final Map<String, Object> metadata = new HashMap<>();
        private PromptTemplate promptTemplate;
        private String outputKey = "llm_response";
        private Consumer<String> tokenHandler;

        private Builder(NodeId id, String name, StreamingChatLanguageModel model) {
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

        public Builder onToken(Consumer<String> handler) {
            this.tokenHandler = handler;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public StreamingLLMNode build() {
            if (promptTemplate == null) {
                throw new IllegalStateException("Prompt template must be set");
            }
            return new StreamingLLMNode(id, name, metadata, model, promptTemplate, outputKey, tokenHandler);
        }
    }
}
