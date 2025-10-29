package dev.langgraph.core.execution;

import dev.langgraph.core.Graph;
import dev.langgraph.core.GraphContext;
import dev.langgraph.core.GraphId;
import dev.langgraph.core.State;
import dev.langgraph.core.event.CompositeEventPublisher;
import dev.langgraph.core.event.EventListener;
import dev.langgraph.core.event.EventPublisher;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class StateGraph {
    
    private final Graph graph;
    private final ExecutionConfig config;
    private final CompositeEventPublisher eventPublisher;
    private final CompositeExecutionHook executionHook;
    private StateGraphExecutor executor;

    private StateGraph(Graph graph, ExecutionConfig config) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.eventPublisher = new CompositeEventPublisher();
        this.executionHook = new CompositeExecutionHook();
        this.executor = new StateGraphExecutor(graph, config, eventPublisher, executionHook);
    }

    public static StateGraph of(Graph graph) {
        return new StateGraph(graph, ExecutionConfig.defaults());
    }

    public static StateGraph of(Graph graph, ExecutionConfig config) {
        return new StateGraph(graph, config);
    }

    public StateGraph addEventListener(EventListener listener) {
        eventPublisher.addListener(listener);
        return this;
    }

    public StateGraph addExecutionHook(ExecutionHook hook) {
        executionHook.addHook(hook);
        return this;
    }

    public ExecutionResult execute(State initialState) {
        GraphContext context = GraphContext.of(graph.id());
        return executor.execute(initialState, context);
    }

    public ExecutionResult execute(State initialState, GraphContext context) {
        return executor.execute(initialState, context);
    }

    public CompletableFuture<ExecutionResult> executeAsync(State initialState) {
        GraphContext context = GraphContext.of(graph.id());
        return executor.executeAsync(initialState, context);
    }

    public CompletableFuture<ExecutionResult> executeAsync(State initialState, GraphContext context) {
        return executor.executeAsync(initialState, context);
    }

    public Graph graph() {
        return graph;
    }

    public ExecutionConfig config() {
        return config;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void shutdownNow() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
