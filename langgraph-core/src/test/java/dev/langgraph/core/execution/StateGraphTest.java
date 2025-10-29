package dev.langgraph.core.execution;

import dev.langgraph.core.*;
import dev.langgraph.core.event.GraphEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StateGraphTest {

    @Test
    void shouldCreateStateGraphFromGraph() {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Test")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        StateGraph stateGraph = StateGraph.of(graph);

        assertNotNull(stateGraph);
        assertEquals(graph, stateGraph.graph());
    }

    @Test
    void shouldExecuteStateGraph() {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Test")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("value", 42))
                .entryPoint(node1)
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertEquals(42, result.finalState().get("value", 0));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldExecuteStateGraphAsync() throws Exception {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Async")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("async", true))
                .entryPoint(node1)
                .build();

        StateGraph stateGraph = StateGraph.of(graph);
        CompletableFuture<ExecutionResult> future = stateGraph.executeAsync(State.empty());

        ExecutionResult result = future.get(5, TimeUnit.SECONDS);

        assertTrue(result.isSuccess());
        assertTrue(result.finalState().has("async"));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldAddEventListeners() {
        List<GraphEvent> events = new ArrayList<>();

        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Events")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        StateGraph stateGraph = StateGraph.of(graph)
                .addEventListener(events::add);

        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> e instanceof GraphEvent.NodeExecutionStarted));
        assertTrue(events.stream().anyMatch(e -> e instanceof GraphEvent.NodeExecutionCompleted));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldAddExecutionHooks() {
        AtomicInteger hookCalls = new AtomicInteger(0);

        ExecutionHook hook = new ExecutionHook() {
            @Override
            public void onExecutionStart(ExecutionContext context, State initialState) {
                hookCalls.incrementAndGet();
            }

            @Override
            public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
                hookCalls.incrementAndGet();
            }
        };

        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Hooks")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        StateGraph stateGraph = StateGraph.of(graph)
                .addExecutionHook(hook);

        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertEquals(2, hookCalls.get());
        
        stateGraph.shutdown();
    }

    @Test
    void shouldExecuteWithCustomConfig() {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Config")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        ExecutionConfig config = ExecutionConfig.builder()
                .maxConcurrency(2)
                .enableSnapshots(true)
                .build();

        StateGraph stateGraph = StateGraph.of(graph, config);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertEquals(config, stateGraph.config());
        
        stateGraph.shutdown();
    }

    @Test
    void shouldExecuteWithGraphContext() {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Context")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    String attr = ctx.graphContext().getAttribute("custom", "default");
                    return state.with("attr", attr);
                })
                .entryPoint(node1)
                .build();

        GraphContext context = GraphContext.of(graph.id())
                .withAttribute("custom", "customValue");

        StateGraph stateGraph = StateGraph.of(graph);
        ExecutionResult result = stateGraph.execute(State.empty(), context);

        assertTrue(result.isSuccess());
        assertEquals("customValue", result.finalState().get("attr", ""));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldHandleMultipleExecutions() {
        NodeId node1 = NodeId.of("node1");
        AtomicInteger counter = new AtomicInteger(0);

        Graph graph = GraphBuilder.newGraph("Multiple")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> 
                    state.with("count", counter.incrementAndGet())
                )
                .entryPoint(node1)
                .build();

        StateGraph stateGraph = StateGraph.of(graph);

        ExecutionResult result1 = stateGraph.execute(State.empty());
        ExecutionResult result2 = stateGraph.execute(State.empty());
        ExecutionResult result3 = stateGraph.execute(State.empty());

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertTrue(result3.isSuccess());
        assertEquals(1, result1.finalState().get("count", 0));
        assertEquals(2, result2.finalState().get("count", 0));
        assertEquals(3, result3.finalState().get("count", 0));
        
        stateGraph.shutdown();
    }
}
