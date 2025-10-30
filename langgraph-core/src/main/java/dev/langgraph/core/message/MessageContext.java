package dev.langgraph.core.message;

import dev.langgraph.core.ExecutionContext;
import dev.langgraph.core.GraphContext;
import dev.langgraph.core.State;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MessageContext {
    
    private final ExecutionContext executionContext;
    private final MessageHistory history;
    private final Map<String, Object> conversationMetadata;

    private MessageContext(ExecutionContext executionContext, MessageHistory history, 
                          Map<String, Object> conversationMetadata) {
        this.executionContext = Objects.requireNonNull(executionContext);
        this.history = Objects.requireNonNull(history);
        this.conversationMetadata = Collections.unmodifiableMap(new HashMap<>(conversationMetadata));
    }

    public static MessageContext create(ExecutionContext executionContext) {
        return new MessageContext(executionContext, MessageHistory.empty(), Map.of());
    }

    public static MessageContext create(ExecutionContext executionContext, MessageHistory history) {
        return new MessageContext(executionContext, history, Map.of());
    }

    public static MessageContext of(ExecutionContext executionContext, MessageHistory history, 
                                   Map<String, Object> conversationMetadata) {
        return new MessageContext(executionContext, history, conversationMetadata);
    }

    public ExecutionContext executionContext() {
        return executionContext;
    }

    public MessageHistory history() {
        return history;
    }

    public Map<String, Object> conversationMetadata() {
        return conversationMetadata;
    }

    public MessageContext withHistory(MessageHistory newHistory) {
        return new MessageContext(executionContext, newHistory, conversationMetadata);
    }

    public MessageContext addMessage(Message message) {
        return new MessageContext(executionContext, history.add(message), conversationMetadata);
    }

    public MessageContext withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(conversationMetadata);
        newMetadata.put(key, value);
        return new MessageContext(executionContext, history, newMetadata);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return (T) conversationMetadata.getOrDefault(key, defaultValue);
    }

    public State toState() {
        return State.empty()
                .with("messages", history.getMessages())
                .with("conversationMetadata", conversationMetadata);
    }

    public static MessageContext fromState(State state, ExecutionContext executionContext) {
        @SuppressWarnings("unchecked")
        List<Message> messages = state.get("messages", List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = state.get("conversationMetadata", Map.of());
        
        return new MessageContext(executionContext, MessageHistory.of(messages), metadata);
    }
}
