# LangGraph Core

Core functionality and abstractions for LangGraph, providing the fundamental building blocks for creating graph-based workflows with language models.

## Core Abstractions

### State Management

- **`State`**: Immutable state representation using Java records
  - Thread-safe and immutable by design
  - Fluent API for creating modified copies
  - Type-safe access to state values
  
- **`StateContainer`**: Thread-safe mutable state container
  - Uses read-write locks for concurrent access
  - Provides atomic update operations
  - Useful for managing shared state across executions

### Graph Structure

- **`GraphId`**: Unique identifier for graphs
- **`NodeId`**: Unique identifier for nodes
- **`EdgeId`**: Unique identifier for edges

### Nodes

- **`Node`**: Core interface for graph nodes
  - `execute()`: Main execution method
  - `onBeforeExecute()`: Lifecycle hook before execution
  - `onAfterExecute()`: Lifecycle hook after execution
  - `onError()`: Error handling callback
  - `canExecute()`: Conditional execution check

- **`AbstractNode`**: Base implementation with lifecycle management
  - Automatic event publishing
  - Exception handling
  - Template method pattern for execution

- **`FunctionalNode`**: Node implementation using functional interfaces
  - Accepts `BiFunction<State, ExecutionContext, State>`
  - Ideal for simple transformations

- **`NodeBuilder`**: Fluent builder for creating nodes

### Edges

- **`Edge`**: Core interface for graph edges
  - `canTraverse()`: Conditional edge traversal
  - `transform()`: State transformation during traversal
  - `priority()`: Edge priority for ordering

- **`AbstractEdge`**: Base implementation for edges
  
- **`DirectEdge`**: Unconditional edge (always traversable)

- **`ConditionalEdge`**: Edge with conditional traversal logic
  - Accepts `BiPredicate<State, ExecutionContext>`

### Graph

- **`Graph`**: Core interface for the graph structure
  - Node and edge management
  - Graph traversal helpers
  - Validation support

- **`DefaultGraph`**: Default implementation
  - Immutable after construction
  - Validates reachability and structure
  - Supports priority-based edge ordering

- **`GraphBuilder`**: Fluent builder for constructing graphs
  - Adds nodes and edges
  - Sets entry point
  - Validates on build

### Context

- **`GraphContext`**: Context for a graph instance
  - Graph ID
  - Custom attributes
  - Immutable with copy-on-write semantics

- **`ExecutionContext`**: Context for a single execution
  - Unique execution ID
  - Start time tracking
  - Metadata support
  - References parent `GraphContext`

### Events

- **`GraphEvent`**: Sealed interface for all graph events
  - `NodeExecutionStarted`
  - `NodeExecutionCompleted`
  - `NodeExecutionFailed`
  - `EdgeTraversed`
  - `StateChanged`
  - `GraphExecutionStarted`
  - `GraphExecutionCompleted`
  - `GraphExecutionFailed`

- **`EventPublisher`**: Interface for publishing events
  - `noOp()`: No-operation publisher for testing

- **`EventListener`**: Functional interface for event listeners

- **`CompositeEventPublisher`**: Multi-listener event publisher
  - Thread-safe listener management
  - Exception isolation (listener errors don't propagate)

## Usage Examples

### Creating a Simple Graph

```java
NodeId node1 = NodeId.of("start");
NodeId node2 = NodeId.of("process");
NodeId node3 = NodeId.of("end");

Graph graph = GraphBuilder.newGraph("MyWorkflow")
    .addFunctionalNode(node1, "Start Node", 
        (state, ctx) -> state.with("started", true))
    .addFunctionalNode(node2, "Process Node",
        (state, ctx) -> state.with("processed", true))
    .addFunctionalNode(node3, "End Node",
        (state, ctx) -> state.with("completed", true))
    .addDirectEdge(node1, node2)
    .addConditionalEdge(node2, node3, 
        (state, ctx) -> state.get("processed").orElse(false))
    .entryPoint(node1)
    .buildAndValidate();
```

### Using Node Builder

```java
Node customNode = NodeBuilder.newNode("CustomNode")
    .metadata("type", "processor")
    .metadata("version", "1.0")
    .function((state, ctx) -> {
        // Custom processing logic
        return state.with("result", "processed");
    })
    .build();
```

### Working with State

```java
// Create immutable state
State state = State.empty()
    .with("key1", "value1")
    .with("key2", 42)
    .withAll(Map.of("key3", true));

// Access values
Optional<String> value = state.get("key1");
Integer number = state.get("key2", 0);

// Thread-safe container
StateContainer container = new StateContainer(state);
container.update("newKey", "newValue");
State current = container.get();
```

### Event Handling

```java
CompositeEventPublisher publisher = new CompositeEventPublisher();

publisher.addListener(event -> {
    System.out.println("Event: " + event);
});

publisher.addListener(event -> {
    if (event instanceof GraphEvent.NodeExecutionFailed failed) {
        System.err.println("Node failed: " + failed.error());
    }
});
```

### Custom Node with Lifecycle Hooks

```java
Node customNode = new AbstractNode(nodeId, "CustomNode", Map.of()) {
    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        // Main execution logic
        return inputState.with("executed", true);
    }
    
    @Override
    public void onBeforeExecute(State state, ExecutionContext context) {
        System.out.println("Starting execution");
    }
    
    @Override
    public void onAfterExecute(State input, State output, ExecutionContext context) {
        System.out.println("Execution completed");
    }
    
    @Override
    public void onError(State state, ExecutionContext context, Throwable error) {
        System.err.println("Execution failed: " + error.getMessage());
    }
};
```

## Thread Safety

- **`State`**: Immutable - inherently thread-safe
- **`StateContainer`**: Thread-safe with read-write locks
- **`CompositeEventPublisher`**: Thread-safe with CopyOnWriteArrayList
- **Graph instances**: Immutable after construction - thread-safe

## Design Principles

1. **Immutability**: Core data structures are immutable by default
2. **Builder Pattern**: Fluent APIs for construction
3. **Event-Driven**: Comprehensive event system for monitoring
4. **Type Safety**: Generic types and Java records where appropriate
5. **Extensibility**: Abstract base classes and interfaces for customization
6. **Validation**: Built-in validation with clear error messages
7. **Thread Safety**: Concurrent execution support where needed

## Testing

The module includes comprehensive unit tests covering:
- State immutability and operations
- Node execution and lifecycle hooks
- Graph construction and validation
- Edge conditions and priorities
- Event publishing and handling
- Thread safety of concurrent operations
- Builder validation rules

Run tests with:
```bash
mvn test -pl langgraph-core
```
