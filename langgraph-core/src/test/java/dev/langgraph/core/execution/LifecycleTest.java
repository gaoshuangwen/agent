package dev.langgraph.core.execution;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {

    @Test
    void shouldTransitionThroughInitRunningCompletedPhases() {
        List<ExecutionPhase> phases = new ArrayList<>();
        
        ExecutionHook hook = new ExecutionHook() {
            @Override
            public void onExecutionStart(ExecutionContext context, State initialState) {
                phases.add(ExecutionPhase.INIT);
            }

            @Override
            public void onNodeStart(ExecutionContext context, NodeId nodeId, State inputState) {
                phases.add(ExecutionPhase.RUNNING);
            }

            @Override
            public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
                phases.add(ExecutionPhase.COMPLETED);
            }
        };

        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Lifecycle")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                hook
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertEquals(ExecutionPhase.COMPLETED, result.phase());
        assertEquals(3, phases.size());
        assertEquals(ExecutionPhase.INIT, phases.get(0));
        assertEquals(ExecutionPhase.RUNNING, phases.get(1));
        assertEquals(ExecutionPhase.COMPLETED, phases.get(2));
        
        executor.shutdown();
    }

    @Test
    void shouldTransitionToFailedPhaseOnError() {
        AtomicBoolean failureHookCalled = new AtomicBoolean(false);
        
        ExecutionHook hook = new ExecutionHook() {
            @Override
            public void onExecutionFailed(ExecutionContext context, Throwable error) {
                failureHookCalled.set(true);
            }
        };

        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("FailureLifecycle")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    throw new RuntimeException("Test failure");
                })
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                hook
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isFailed());
        assertEquals(ExecutionPhase.FAILED, result.phase());
        assertTrue(failureHookCalled.get());
        assertTrue(result.error().isPresent());
        
        executor.shutdown();
    }

    @Test
    void shouldCallNodeLifecycleHooks() {
        AtomicInteger beforeCount = new AtomicInteger(0);
        AtomicInteger afterCount = new AtomicInteger(0);

        NodeId node1 = NodeId.of("node1");
        
        Node customNode = new AbstractNode(node1, "Custom", java.util.Map.of()) {
            @Override
            protected State doExecute(State inputState, ExecutionContext context) {
                return inputState.with("executed", true);
            }

            @Override
            public void onBeforeExecute(State state, ExecutionContext context) {
                beforeCount.incrementAndGet();
            }

            @Override
            public void onAfterExecute(State inputState, State outputState, ExecutionContext context) {
                afterCount.incrementAndGet();
            }
        };

        Graph graph = GraphBuilder.newGraph("NodeLifecycle")
                .addNode(customNode)
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertEquals(1, beforeCount.get());
        assertEquals(1, afterCount.get());
        
        executor.shutdown();
    }

    @Test
    void shouldCallErrorHookOnNodeFailure() {
        AtomicBoolean errorHookCalled = new AtomicBoolean(false);
        AtomicBoolean onErrorCalled = new AtomicBoolean(false);

        NodeId node1 = NodeId.of("node1");
        
        Node customNode = new AbstractNode(node1, "Custom", java.util.Map.of()) {
            @Override
            protected State doExecute(State inputState, ExecutionContext context) {
                throw new RuntimeException("Node error");
            }

            @Override
            public void onError(State state, ExecutionContext context, Throwable error) {
                onErrorCalled.set(true);
            }
        };

        ExecutionHook hook = new ExecutionHook() {
            @Override
            public void onNodeFailed(ExecutionContext context, NodeId nodeId, Throwable error, int attemptNumber) {
                errorHookCalled.set(true);
            }
        };

        Graph graph = GraphBuilder.newGraph("ErrorHooks")
                .addNode(customNode)
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                hook
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isFailed());
        assertTrue(errorHookCalled.get());
        assertTrue(onErrorCalled.get());
        
        executor.shutdown();
    }

    @Test
    void shouldTrackExecutionDuration() throws InterruptedException {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Duration")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return state.with("done", true);
                })
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertTrue(result.duration().toMillis() >= 100);
        assertTrue(result.endTime().isPresent());
        
        executor.shutdown();
    }

    @Test
    void shouldExecuteRetryLifecycle() {
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicInteger attemptCount = new AtomicInteger(0);

        ExecutionHook hook = new ExecutionHook() {
            @Override
            public void onNodeRetry(ExecutionContext context, NodeId nodeId, int attemptNumber) {
                retryCount.incrementAndGet();
            }

            @Override
            public void onNodeFailed(ExecutionContext context, NodeId nodeId, Throwable error, int attemptNumber) {
                // Called on each failure
            }
        };

        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("RetryLifecycle")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    int attempt = attemptCount.incrementAndGet();
                    if (attempt < 3) {
                        throw new RuntimeException("Retry needed");
                    }
                    return state.with("success", true);
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
                hook
        );

        ExecutionResult result = executor.execute(State.empty(), GraphContext.of(GraphId.generate()));

        assertTrue(result.isSuccess());
        assertEquals(2, retryCount.get());
        assertEquals(3, attemptCount.get());
        
        executor.shutdown();
    }
}
