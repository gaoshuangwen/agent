package dev.langgraph.core.execution;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StateGraphExecutorTest {

    @Test
    void shouldExecuteSimpleLinearGraph() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId node3 = NodeId.of("node3");

        Graph graph = GraphBuilder.newGraph("Linear")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step1", true))
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state.with("step2", true))
                .addFunctionalNode(node3, "Node3", (state, ctx) -> state.with("step3", true))
                .addDirectEdge(node1, node2)
                .addDirectEdge(node2, node3)
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        State initialState = State.empty();
        GraphContext context = GraphContext.of(GraphId.generate());
        ExecutionResult result = executor.execute(initialState, context);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailed());
        assertTrue(result.finalState().has("step1"));
        assertTrue(result.finalState().has("step2"));
        assertTrue(result.finalState().has("step3"));
        
        executor.shutdown();
    }

    @Test
    void shouldExecuteConditionalGraph() {
        NodeId start = NodeId.of("start");
        NodeId pathA = NodeId.of("pathA");
        NodeId pathB = NodeId.of("pathB");

        Graph graph = GraphBuilder.newGraph("Conditional")
                .addFunctionalNode(start, "Start", (state, ctx) -> state.with("value", 10))
                .addFunctionalNode(pathA, "PathA", (state, ctx) -> state.with("pathA", true))
                .addFunctionalNode(pathB, "PathB", (state, ctx) -> state.with("pathB", true))
                .addConditionalEdge(start, pathA, (state, ctx) -> {
                    Integer value = state.get("value", 0);
                    return value >= 10;
                })
                .addConditionalEdge(start, pathB, (state, ctx) -> {
                    Integer value = state.get("value", 0);
                    return value < 10;
                })
                .entryPoint(start)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertTrue(result.finalState().has("pathA"));
        assertFalse(result.finalState().has("pathB"));
        
        executor.shutdown();
    }

    @Test
    void shouldHandleNodeFailure() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph("Failing")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step1", true))
                .addFunctionalNode(node2, "Node2", (state, ctx) -> {
                    throw new RuntimeException("Node failure");
                })
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isFailed());
        assertTrue(result.error().isPresent());
        assertEquals("Node failure", result.error().get().getMessage());
        
        executor.shutdown();
    }

    @Test
    void shouldExecuteWithRetryPolicy() {
        AtomicInteger attempts = new AtomicInteger(0);
        NodeId node1 = NodeId.of("node1");

        Graph graph = GraphBuilder.newGraph("Retry")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    int attempt = attempts.incrementAndGet();
                    if (attempt < 3) {
                        throw new RuntimeException("Retry me");
                    }
                    return state.with("attempts", attempt);
                })
                .entryPoint(node1)
                .build();

        ExecutionConfig config = ExecutionConfig.builder()
                .retryPolicy(RetryPolicy.fixedRetry(3, java.time.Duration.ofMillis(10)))
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                config,
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertEquals(3, result.finalState().get("attempts", 0));
        
        executor.shutdown();
    }

    @Test
    void shouldCaptureStateSnapshots() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph("Snapshots")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step1", true))
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state.with("step2", true))
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        SnapshotStore snapshotStore = SnapshotStore.inMemory();
        ExecutionConfig config = ExecutionConfig.builder()
                .enableSnapshots(true)
                .snapshotStore(snapshotStore)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                config,
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertEquals(2, snapshotStore.getAll(result.executionId()).size());
        
        executor.shutdown();
    }

    @Test
    void shouldInvokeExecutionHooks() {
        AtomicInteger startCount = new AtomicInteger(0);
        AtomicInteger completeCount = new AtomicInteger(0);
        AtomicInteger nodeStartCount = new AtomicInteger(0);
        AtomicInteger nodeCompleteCount = new AtomicInteger(0);

        ExecutionHook hook = new ExecutionHook() {
            @Override
            public void onExecutionStart(ExecutionContext context, State initialState) {
                startCount.incrementAndGet();
            }

            @Override
            public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
                completeCount.incrementAndGet();
            }

            @Override
            public void onNodeStart(ExecutionContext context, NodeId nodeId, State inputState) {
                nodeStartCount.incrementAndGet();
            }

            @Override
            public void onNodeComplete(ExecutionContext context, NodeId nodeId, State outputState) {
                nodeCompleteCount.incrementAndGet();
            }
        };

        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Hooks")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                hook
        );

        executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertEquals(1, startCount.get());
        assertEquals(1, completeCount.get());
        assertEquals(1, nodeStartCount.get());
        assertEquals(1, nodeCompleteCount.get());
        
        executor.shutdown();
    }
}
