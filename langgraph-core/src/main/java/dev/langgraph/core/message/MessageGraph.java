package dev.langgraph.core.message;

import dev.langgraph.core.Graph;
import dev.langgraph.core.GraphContext;
import dev.langgraph.core.State;
import dev.langgraph.core.execution.ExecutionConfig;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateGraph;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class MessageGraph {
    
    private final StateGraph stateGraph;
    private final int maxHistorySize;

    private MessageGraph(StateGraph stateGraph, int maxHistorySize) {
        this.stateGraph = Objects.requireNonNull(stateGraph);
        this.maxHistorySize = maxHistorySize;
    }

    public static MessageGraph of(Graph graph) {
        return new MessageGraph(StateGraph.of(graph), Integer.MAX_VALUE);
    }

    public static MessageGraph of(Graph graph, ExecutionConfig config) {
        return new MessageGraph(StateGraph.of(graph, config), Integer.MAX_VALUE);
    }

    public static MessageGraph of(Graph graph, ExecutionConfig config, int maxHistorySize) {
        return new MessageGraph(StateGraph.of(graph, config), maxHistorySize);
    }

    public ExecutionResult execute(List<Message> initialMessages) {
        MessageHistory history = MessageHistory.withMaxSize(maxHistorySize);
        for (Message msg : initialMessages) {
            history = history.add(msg);
        }
        
        State initialState = State.empty()
                .with("messages", history.getMessages())
                .with("conversationMetadata", java.util.Map.of());
        
        return stateGraph.execute(initialState);
    }

    public ExecutionResult execute(Message initialMessage) {
        return execute(List.of(initialMessage));
    }

    public ExecutionResult execute(String userMessage) {
        return execute(Message.user(userMessage));
    }

    public ExecutionResult continueConversation(ExecutionResult previousResult, Message newMessage) {
        State previousState = previousResult.finalState();
        MessageHistory history = extractHistory(previousState);
        history = history.add(newMessage);
        
        State newState = State.empty()
                .with("messages", history.getMessages())
                .with("conversationMetadata", previousState.get("conversationMetadata", java.util.Map.of()));
        
        return stateGraph.execute(newState);
    }

    public CompletableFuture<ExecutionResult> executeAsync(List<Message> initialMessages) {
        MessageHistory history = MessageHistory.withMaxSize(maxHistorySize);
        for (Message msg : initialMessages) {
            history = history.add(msg);
        }
        
        State initialState = State.empty()
                .with("messages", history.getMessages())
                .with("conversationMetadata", java.util.Map.of());
        
        return stateGraph.executeAsync(initialState);
    }

    public CompletableFuture<ExecutionResult> executeAsync(Message initialMessage) {
        return executeAsync(List.of(initialMessage));
    }

    public CompletableFuture<ExecutionResult> executeAsync(String userMessage) {
        return executeAsync(Message.user(userMessage));
    }

    public MessageHistory extractHistory(ExecutionResult result) {
        return extractHistory(result.finalState());
    }

    @SuppressWarnings("unchecked")
    private MessageHistory extractHistory(State state) {
        List<Message> messages = state.get("messages", List.of());
        return MessageHistory.of(messages);
    }

    public StateGraph stateGraph() {
        return stateGraph;
    }

    public void shutdown() {
        stateGraph.shutdown();
    }

    public void shutdownNow() {
        stateGraph.shutdownNow();
    }
}
