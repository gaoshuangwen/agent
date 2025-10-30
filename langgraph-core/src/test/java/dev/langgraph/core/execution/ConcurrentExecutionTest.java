package dev.langgraph.core.execution;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentExecutionTest {

    @Test
    void shouldExecuteMultipleGraphsConcurrently() throws Exception {
        NodeId node1 = NodeId.of("node1");
        Graph graph = GraphBuilder.newGraph("Concurrent")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return state.with("completed", true);
                })
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        int numExecutions = 10;
        List<CompletableFuture<ExecutionResult>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numExecutions; i++) {
            CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(() -> 
                executor.execute(State.empty(), GraphContext.of(GraphId.generate()))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        for (CompletableFuture<ExecutionResult> future : futures) {
            ExecutionResult result = future.get();
            assertTrue(result.isSuccess());
            assertTrue(result.finalState().has("completed"));
        }
        
        executor.shutdown();
    }

    @Test
    void shouldRespectConcurrencyLimits() throws Exception {
        AtomicInteger concurrentExecutions = new AtomicInteger(0);
        AtomicInteger maxConcurrentExecutions = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId node3 = NodeId.of("node3");

        Graph graph = GraphBuilder.newGraph("Concurrency")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    int current = concurrentExecutions.incrementAndGet();
                    maxConcurrentExecutions.updateAndGet(max -> Math.max(max, current));
                    try {
                        latch.await(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        concurrentExecutions.decrementAndGet();
                    }
                    return state.with("node1", true);
                })
                .addFunctionalNode(node2, "Node2", (state, ctx) -> {
                    int current = concurrentExecutions.incrementAndGet();
                    maxConcurrentExecutions.updateAndGet(max -> Math.max(max, current));
                    try {
                        latch.await(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        concurrentExecutions.decrementAndGet();
                    }
                    return state.with("node2", true);
                })
                .addFunctionalNode(node3, "Node3", (state, ctx) -> {
                    int current = concurrentExecutions.incrementAndGet();
                    maxConcurrentExecutions.updateAndGet(max -> Math.max(max, current));
                    try {
                        latch.await(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        concurrentExecutions.decrementAndGet();
                    }
                    return state.with("node3", true);
                })
                .addConditionalEdge(node1, node2, (s, c) -> true)
                .addConditionalEdge(node1, node3, (s, c) -> true)
                .entryPoint(node1)
                .build();

        ExecutionConfig config = ExecutionConfig.builder()
                .maxConcurrency(2)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                config,
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(() ->
                executor.execute(State.empty(), GraphContext.of(GraphId.generate()))
        );

        Thread.sleep(50);
        latch.countDown();

        ExecutionResult result = future.get(2, TimeUnit.SECONDS);
        assertTrue(result.isSuccess());
        assertTrue(maxConcurrentExecutions.get() <= 2, 
                "Max concurrent executions was " + maxConcurrentExecutions.get() + ", expected <= 2");
        
        executor.shutdown();
    }

    @Test
    void shouldHandleParallelNodeExecution() {
        NodeId start = NodeId.of("start");
        NodeId parallel1 = NodeId.of("parallel1");
        NodeId parallel2 = NodeId.of("parallel2");

        AtomicInteger executionCount = new AtomicInteger(0);

        Graph graph = GraphBuilder.newGraph("Parallel")
                .addFunctionalNode(start, "Start", (state, ctx) -> state.with("started", true))
                .addFunctionalNode(parallel1, "Parallel1", (state, ctx) -> {
                    executionCount.incrementAndGet();
                    return state.with("parallel1", true);
                })
                .addFunctionalNode(parallel2, "Parallel2", (state, ctx) -> {
                    executionCount.incrementAndGet();
                    return state.with("parallel2", true);
                })
                .addConditionalEdge(start, parallel1, (s, c) -> true)
                .addConditionalEdge(start, parallel2, (s, c) -> true)
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
        assertEquals(2, executionCount.get());
        
        executor.shutdown();
    }

    @Test
    void shouldMaintainThreadSafety() throws Exception {
        NodeId node1 = NodeId.of("node1");
        AtomicInteger counter = new AtomicInteger(0);

        Graph graph = GraphBuilder.newGraph("ThreadSafe")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> {
                    int value = counter.incrementAndGet();
                    return state.with("counter", value);
                })
                .entryPoint(node1)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(
                graph,
                ExecutionConfig.defaults(),
                EventPublisher.noOp(),
                ExecutionHook.noOp()
        );

        int numThreads = 20;
        List<CompletableFuture<ExecutionResult>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            CompletableFuture<ExecutionResult> future = CompletableFuture.supplyAsync(() ->
                    executor.execute(State.empty(), GraphContext.of(GraphId.generate()))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

        assertEquals(numThreads, counter.get());

        for (CompletableFuture<ExecutionResult> future : futures) {
            assertTrue(future.get().isSuccess());
        }
        
        executor.shutdown();
    }
}
