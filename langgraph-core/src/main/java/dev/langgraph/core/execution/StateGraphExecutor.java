package dev.langgraph.core.execution;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class StateGraphExecutor {
    
    private final Graph graph;
    private final ExecutionConfig config;
    private final EventPublisher eventPublisher;
    private final ExecutionHook hook;
    private final ExecutorService executorService;

    public StateGraphExecutor(Graph graph, ExecutionConfig config, 
                             EventPublisher eventPublisher, ExecutionHook hook) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "Event publisher cannot be null");
        this.hook = Objects.requireNonNull(hook, "Hook cannot be null");
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public ExecutionResult execute(State initialState, GraphContext graphContext) {
        ExecutionContext context = ExecutionContext.create(graphContext);
        Instant startTime = Instant.now();
        
        hook.onExecutionStart(context, initialState);

        try {
            State finalState = executeGraph(initialState, context);
            Instant endTime = Instant.now();
            ExecutionResult result = ExecutionResult.success(context.executionId(), finalState, startTime, endTime);
            hook.onExecutionComplete(context, result);
            return result;
        } catch (CancellationException e) {
            Instant endTime = Instant.now();
            ExecutionResult result = ExecutionResult.cancelled(context.executionId(), initialState, startTime, endTime);
            hook.onExecutionCancelled(context);
            return result;
        } catch (Exception e) {
            Instant endTime = Instant.now();
            ExecutionResult result = ExecutionResult.failed(context.executionId(), initialState, startTime, endTime, e);
            hook.onExecutionFailed(context, e);
            return result;
        }
    }

    public CompletableFuture<ExecutionResult> executeAsync(State initialState, GraphContext graphContext) {
        return CompletableFuture.supplyAsync(() -> execute(initialState, graphContext), executorService);
    }

    private State executeGraph(State initialState, ExecutionContext context) throws Exception {
        NodeId entryPoint = graph.entryPoint()
                .orElseThrow(() -> new IllegalStateException("Graph has no entry point"));

        StateContainer stateContainer = new StateContainer(initialState);
        Set<NodeId> visitedNodes = ConcurrentHashMap.newKeySet();
        Semaphore concurrencyLimit = new Semaphore(config.maxConcurrency());

        return executeNode(entryPoint, stateContainer, visitedNodes, context, concurrencyLimit);
    }

    private State executeNode(NodeId nodeId, StateContainer stateContainer, Set<NodeId> visitedNodes,
                             ExecutionContext context, Semaphore concurrencyLimit) throws Exception {
        if (visitedNodes.contains(nodeId)) {
            return stateContainer.get();
        }

        visitedNodes.add(nodeId);
        hook.onNodeScheduled(context, nodeId);

        Node node = graph.getNode(nodeId)
                .orElseThrow(() -> new IllegalStateException("Node not found: " + nodeId));

        concurrencyLimit.acquire();
        try {
            State inputState = stateContainer.get();
            
            if (!node.canExecute(inputState, context)) {
                return inputState;
            }

            hook.onNodeStart(context, nodeId, inputState);
            State outputState = executeNodeWithRetry(node, inputState, context);
            stateContainer.set(outputState);
            hook.onNodeComplete(context, nodeId, outputState);

            if (config.enableSnapshots()) {
                StateSnapshot snapshot = StateSnapshot.create(context.executionId(), nodeId, outputState);
                config.snapshotStore().save(snapshot);
                hook.onStateSnapshot(context, snapshot);
            }

            List<Edge> outgoingEdges = graph.getOutgoingEdges(nodeId);
            if (outgoingEdges.isEmpty()) {
                return outputState;
            }

            return executeOutgoingEdges(outgoingEdges, stateContainer, visitedNodes, context, concurrencyLimit);
        } finally {
            concurrencyLimit.release();
        }
    }

    private State executeNodeWithRetry(Node node, State inputState, ExecutionContext context) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        while (true) {
            attempt++;
            try {
                return node.execute(inputState, context, eventPublisher);
            } catch (Exception e) {
                lastException = e;
                hook.onNodeFailed(context, node.id(), e, attempt);

                if (!config.retryPolicy().shouldRetry(attempt, e)) {
                    throw e;
                }

                hook.onNodeRetry(context, node.id(), attempt);
                Thread.sleep(config.retryPolicy().getBackoffDuration(attempt).toMillis());
            }
        }
    }

    private State executeOutgoingEdges(List<Edge> edges, StateContainer stateContainer, Set<NodeId> visitedNodes,
                                      ExecutionContext context, Semaphore concurrencyLimit) throws Exception {
        State currentState = stateContainer.get();
        
        List<Edge> traversableEdges = edges.stream()
                .filter(edge -> edge.canTraverse(currentState, context))
                .toList();

        if (traversableEdges.isEmpty()) {
            return currentState;
        }

        if (traversableEdges.size() == 1) {
            Edge edge = traversableEdges.get(0);
            State transformedState = edge.transform(currentState, context);
            stateContainer.set(transformedState);
            return executeNode(edge.target(), stateContainer, visitedNodes, context, concurrencyLimit);
        }

        return executeParallelEdges(traversableEdges, stateContainer, visitedNodes, context, concurrencyLimit);
    }

    private State executeParallelEdges(List<Edge> edges, StateContainer stateContainer, Set<NodeId> visitedNodes,
                                      ExecutionContext context, Semaphore concurrencyLimit) throws Exception {
        AtomicReference<Exception> firstError = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(edges.size());
        List<CompletableFuture<State>> futures = new ArrayList<>();

        for (Edge edge : edges) {
            CompletableFuture<State> future = CompletableFuture.supplyAsync(() -> {
                try {
                    State transformedState = edge.transform(stateContainer.get(), context);
                    StateContainer edgeState = new StateContainer(transformedState);
                    return executeNode(edge.target(), edgeState, visitedNodes, context, concurrencyLimit);
                } catch (Exception e) {
                    firstError.compareAndSet(null, e);
                    throw new CompletionException(e);
                } finally {
                    latch.countDown();
                }
            }, executorService);
            futures.add(future);
        }

        try {
            latch.await(config.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            futures.forEach(f -> f.cancel(true));
            Thread.currentThread().interrupt();
            throw new CancellationException("Execution interrupted");
        }

        if (firstError.get() != null) {
            throw firstError.get();
        }

        for (CompletableFuture<State> future : futures) {
            State result = future.getNow(stateContainer.get());
            stateContainer.set(result);
        }

        return stateContainer.get();
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void shutdownNow() {
        executorService.shutdownNow();
    }
}
