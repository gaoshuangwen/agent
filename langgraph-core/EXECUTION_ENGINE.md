# StateGraph Execution Engine

Comprehensive execution framework for running graph-based workflows with support for parallel execution, lifecycle management, retry policies, state snapshots, and observability hooks.

## Core Components

### StateGraph

High-level API for executing graphs with automatic lifecycle management.

```java
Graph graph = GraphBuilder.newGraph("MyWorkflow")
    .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step", 1))
    .addFunctionalNode(node2, "Node2", (state, ctx) -> state.with("step", 2))
    .addDirectEdge(node1, node2)
    .entryPoint(node1)
    .build();

StateGraph stateGraph = StateGraph.of(graph);
ExecutionResult result = stateGraph.execute(State.empty());

if (result.isSuccess()) {
    System.out.println("Final state: " + result.finalState());
}
```

### StateGraphExecutor

Low-level execution engine with full control over configuration.

```java
StateGraphExecutor executor = new StateGraphExecutor(
    graph,
    ExecutionConfig.defaults(),
    EventPublisher.noOp(),
    ExecutionHook.noOp()
);

ExecutionResult result = executor.execute(initialState, graphContext);
executor.shutdown();
```

## Execution Phases

The execution engine manages the following lifecycle phases:

1. **INIT**: Execution initialization
2. **RUNNING**: Active execution
3. **COMPLETED**: Successful completion
4. **FAILED**: Execution failed with error
5. **CANCELLED**: Execution was cancelled

## Configuration

### ExecutionConfig

Configure execution behavior:

```java
ExecutionConfig config = ExecutionConfig.builder()
    .maxConcurrency(4)                    // Max parallel nodes
    .timeout(Duration.ofMinutes(10))       // Execution timeout
    .retryPolicy(RetryPolicy.fixedRetry(3, Duration.ofSeconds(1)))
    .enableSnapshots(true)                 // Enable state snapshots
    .snapshotStore(SnapshotStore.inMemory())
    .build();

StateGraph stateGraph = StateGraph.of(graph, config);
```

### Retry Policies

Three retry policy implementations:

#### No Retry
```java
RetryPolicy.noRetry()
```

#### Fixed Retry
```java
RetryPolicy.fixedRetry(maxAttempts, backoffDuration)
```

#### Exponential Backoff
```java
RetryPolicy.exponentialBackoff(maxAttempts, initialBackoff, multiplier)
```

Example with retry:
```java
ExecutionConfig config = ExecutionConfig.builder()
    .retryPolicy(RetryPolicy.exponentialBackoff(
        5,                              // max 5 attempts
        Duration.ofMillis(100),         // start with 100ms
        2.0                             // double each time
    ))
    .build();
```

## Concurrency Control

### Virtual Threads

The execution engine uses Java 21 virtual threads for efficient parallel execution:

```java
// Parallel node execution with concurrency limits
ExecutionConfig config = ExecutionConfig.builder()
    .maxConcurrency(10)  // Max 10 nodes executing concurrently
    .build();
```

### Parallel Edge Execution

When multiple edges from a node are traversable, they execute in parallel:

```java
Graph graph = GraphBuilder.newGraph()
    .addFunctionalNode(start, "Start", (state, ctx) -> state)
    .addFunctionalNode(pathA, "PathA", (state, ctx) -> state.with("a", true))
    .addFunctionalNode(pathB, "PathB", (state, ctx) -> state.with("b", true))
    .addConditionalEdge(start, pathA, (s, c) -> true)  // Both edges
    .addConditionalEdge(start, pathB, (s, c) -> true)  // execute in parallel
    .entryPoint(start)
    .build();
```

## State Snapshots

Capture state at execution boundaries for debugging and recovery:

```java
ExecutionConfig config = ExecutionConfig.builder()
    .enableSnapshots(true)
    .snapshotStore(SnapshotStore.inMemory())
    .build();

StateGraph stateGraph = StateGraph.of(graph, config);
ExecutionResult result = stateGraph.execute(State.empty());

// Retrieve snapshots
SnapshotStore store = config.snapshotStore();
List<StateSnapshot> snapshots = store.getAll(result.executionId());

for (StateSnapshot snapshot : snapshots) {
    System.out.println("Node: " + snapshot.nodeId());
    System.out.println("State: " + snapshot.state());
    System.out.println("Time: " + snapshot.timestamp());
}
```

## Execution Hooks

Monitor execution with comprehensive lifecycle hooks:

```java
ExecutionHook hook = new ExecutionHook() {
    @Override
    public void onExecutionStart(ExecutionContext context, State initialState) {
        log.info("Execution started: {}", context.executionId());
    }

    @Override
    public void onNodeStart(ExecutionContext context, NodeId nodeId, State inputState) {
        log.info("Node {} starting", nodeId);
    }

    @Override
    public void onNodeComplete(ExecutionContext context, NodeId nodeId, State outputState) {
        log.info("Node {} completed", nodeId);
    }

    @Override
    public void onNodeFailed(ExecutionContext context, NodeId nodeId, 
                            Throwable error, int attemptNumber) {
        log.error("Node {} failed on attempt {}", nodeId, attemptNumber, error);
    }

    @Override
    public void onNodeRetry(ExecutionContext context, NodeId nodeId, int attemptNumber) {
        log.warn("Retrying node {} (attempt {})", nodeId, attemptNumber);
    }

    @Override
    public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
        log.info("Execution completed in {}", result.duration());
    }

    @Override
    public void onExecutionFailed(ExecutionContext context, Throwable error) {
        log.error("Execution failed", error);
    }
};

StateGraph stateGraph = StateGraph.of(graph)
    .addExecutionHook(hook);
```

## Event Listeners

Listen to graph execution events:

```java
StateGraph stateGraph = StateGraph.of(graph)
    .addEventListener(event -> {
        if (event instanceof GraphEvent.NodeExecutionStarted started) {
            System.out.println("Node started: " + started.nodeId());
        }
    });
```

## Async Execution

Execute graphs asynchronously:

```java
StateGraph stateGraph = StateGraph.of(graph);

CompletableFuture<ExecutionResult> future = stateGraph.executeAsync(State.empty());

future.thenAccept(result -> {
    if (result.isSuccess()) {
        System.out.println("Execution completed successfully");
    }
});

// Or wait for completion
ExecutionResult result = future.get(30, TimeUnit.SECONDS);
```

## Error Handling

### Automatic Error Propagation

Errors in nodes automatically propagate to the execution result:

```java
ExecutionResult result = stateGraph.execute(State.empty());

if (result.isFailed()) {
    Throwable error = result.error().orElse(null);
    System.err.println("Execution failed: " + error.getMessage());
}
```

### Node-Level Error Handling

Implement custom error handling in nodes:

```java
Node node = new AbstractNode(nodeId, "CustomNode", Map.of()) {
    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        // Execution logic
        return inputState;
    }

    @Override
    public void onError(State state, ExecutionContext context, Throwable error) {
        // Custom error handling
        log.error("Node error", error);
    }
};
```

## Execution Results

The `ExecutionResult` provides comprehensive execution information:

```java
ExecutionResult result = stateGraph.execute(State.empty());

// Execution status
boolean success = result.isSuccess();
boolean failed = result.isFailed();
boolean cancelled = result.isCancelled();

// Execution details
String executionId = result.executionId();
ExecutionPhase phase = result.phase();
Duration duration = result.duration();

// State and errors
State finalState = result.finalState();
Optional<Throwable> error = result.error();

// Timing
Instant startTime = result.startTime();
Optional<Instant> endTime = result.endTime();
```

## Thread Safety

### Guarantees

- **StateGraphExecutor**: Thread-safe for concurrent executions
- **StateGraph**: Thread-safe for multiple executions
- **StateContainer**: Thread-safe with read-write locks
- **SnapshotStore**: Thread-safe implementations provided
- **ExecutionHooks**: Automatically isolated from failures

### Concurrent Executions

Execute multiple graphs concurrently:

```java
StateGraph stateGraph = StateGraph.of(graph);

List<CompletableFuture<ExecutionResult>> futures = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    State initialState = State.of(Map.of("id", i));
    futures.add(stateGraph.executeAsync(initialState));
}

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
```

## Performance Considerations

### Virtual Threads

- Uses Java 21 virtual threads for efficient concurrency
- Minimal overhead for parallel node execution
- Scales to thousands of concurrent executions

### Concurrency Limits

Set appropriate concurrency limits based on:
- Available CPU cores
- I/O characteristics of nodes
- Memory constraints

```java
ExecutionConfig config = ExecutionConfig.builder()
    .maxConcurrency(Runtime.getRuntime().availableProcessors())
    .build();
```

### Snapshot Performance

Snapshots add overhead. Use judiciously:

```java
// Enable only for debugging or critical workflows
ExecutionConfig config = ExecutionConfig.builder()
    .enableSnapshots(true)  // Adds overhead
    .build();
```

## Best Practices

### 1. Resource Management

Always shutdown executors:

```java
StateGraph stateGraph = StateGraph.of(graph);
try {
    ExecutionResult result = stateGraph.execute(State.empty());
    // Use result
} finally {
    stateGraph.shutdown();
}
```

### 2. Timeout Configuration

Set appropriate timeouts:

```java
ExecutionConfig config = ExecutionConfig.builder()
    .timeout(Duration.ofMinutes(5))  // Prevent runaway executions
    .build();
```

### 3. Error Handling

Always check execution results:

```java
ExecutionResult result = stateGraph.execute(State.empty());

if (result.isFailed()) {
    handleError(result.error().orElseThrow());
} else if (result.isCancelled()) {
    handleCancellation();
} else {
    processResult(result.finalState());
}
```

### 4. Observability

Use hooks for monitoring:

```java
ExecutionHook metricsHook = new ExecutionHook() {
    @Override
    public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
        metrics.record("execution.duration", result.duration().toMillis());
        metrics.increment("execution.success");
    }

    @Override
    public void onExecutionFailed(ExecutionContext context, Throwable error) {
        metrics.increment("execution.failure");
        metrics.increment("execution.failure." + error.getClass().getSimpleName());
    }
};
```

### 5. State Management

Keep state immutable:

```java
// Good: Returns new state
(state, ctx) -> state.with("newKey", "newValue")

// Bad: Mutates state (won't work - state is immutable)
(state, ctx) -> {
    state.data().put("key", "value");  // Throws UnsupportedOperationException
    return state;
}
```

## Testing

Comprehensive test coverage for:
- Simple linear graphs
- Conditional branching
- Parallel execution
- Error handling and retries
- Lifecycle transitions
- Thread safety
- Snapshot functionality

Run tests:
```bash
mvn test -pl langgraph-core -Dtest="*execution*"
```

## Examples

### Simple Linear Workflow

```java
NodeId node1 = NodeId.of("validate");
NodeId node2 = NodeId.of("process");
NodeId node3 = NodeId.of("save");

Graph graph = GraphBuilder.newGraph("Pipeline")
    .addFunctionalNode(node1, "Validate", (state, ctx) -> {
        // Validation logic
        return state.with("validated", true);
    })
    .addFunctionalNode(node2, "Process", (state, ctx) -> {
        // Processing logic
        return state.with("processed", true);
    })
    .addFunctionalNode(node3, "Save", (state, ctx) -> {
        // Save logic
        return state.with("saved", true);
    })
    .addDirectEdge(node1, node2)
    .addDirectEdge(node2, node3)
    .entryPoint(node1)
    .build();

StateGraph stateGraph = StateGraph.of(graph);
ExecutionResult result = stateGraph.execute(State.of(Map.of("input", "data")));
```

### Conditional Workflow

```java
NodeId decision = NodeId.of("decision");
NodeId pathA = NodeId.of("pathA");
NodeId pathB = NodeId.of("pathB");

Graph graph = GraphBuilder.newGraph("Conditional")
    .addFunctionalNode(decision, "Decision", (state, ctx) -> 
        state.with("value", 15))
    .addFunctionalNode(pathA, "HighValue", (state, ctx) -> 
        state.with("category", "high"))
    .addFunctionalNode(pathB, "LowValue", (state, ctx) -> 
        state.with("category", "low"))
    .addConditionalEdge(decision, pathA, (state, ctx) -> 
        state.get("value", 0) >= 10)
    .addConditionalEdge(decision, pathB, (state, ctx) -> 
        state.get("value", 0) < 10)
    .entryPoint(decision)
    .build();
```

### Parallel Processing

```java
NodeId start = NodeId.of("start");
NodeId task1 = NodeId.of("task1");
NodeId task2 = NodeId.of("task2");
NodeId task3 = NodeId.of("task3");

Graph graph = GraphBuilder.newGraph("Parallel")
    .addFunctionalNode(start, "Start", (state, ctx) -> state)
    .addFunctionalNode(task1, "Task1", (state, ctx) -> 
        state.with("task1", "done"))
    .addFunctionalNode(task2, "Task2", (state, ctx) -> 
        state.with("task2", "done"))
    .addFunctionalNode(task3, "Task3", (state, ctx) -> 
        state.with("task3", "done"))
    .addConditionalEdge(start, task1, (s, c) -> true)
    .addConditionalEdge(start, task2, (s, c) -> true)
    .addConditionalEdge(start, task3, (s, c) -> true)
    .entryPoint(start)
    .build();

// All tasks execute in parallel
```
