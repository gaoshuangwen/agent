# Core Abstractions Implementation Notes

## Overview

This document describes the core abstractions implemented in the `langgraph-core` module for the LangGraph Java project.

## Implemented Components

### 1. Core Identifiers (Records)
- **`GraphId`**: Unique identifier for graph instances
- **`NodeId`**: Unique identifier for nodes within a graph
- **`EdgeId`**: Unique identifier for edges between nodes

All IDs support:
- Creation from string values
- Auto-generation using UUIDs
- Validation (non-null, non-blank)
- Proper equals/hashCode implementation

### 2. State Management

#### State (Immutable)
- Java record providing immutable state representation
- Map-based data storage with type-safe access
- Fluent API for creating modified copies: `with()`, `withAll()`, `without()`
- Thread-safe by design (immutability)
- Generic getter methods with Optional support

#### StateContainer (Thread-Safe)
- Mutable container for State with read-write lock protection
- Atomic operations: `update()`, `remove()`, `get()`, `set()`
- Thread-safe for concurrent access
- Suitable for shared state across multiple threads

### 3. Node Abstraction

#### Node Interface
- Core contract for all graph nodes
- Methods:
  - `execute()`: Main execution with state transformation
  - `onBeforeExecute()`: Pre-execution hook
  - `onAfterExecute()`: Post-execution hook
  - `onError()`: Error handling callback
  - `canExecute()`: Conditional execution check
- Supports metadata attachment
- Fully typed state transitions

#### AbstractNode
- Template implementation of Node interface
- Automatic lifecycle management
- Event publishing integration
- Exception handling with error hooks
- Extensible via `doExecute()` method

#### FunctionalNode
- Concrete implementation accepting functional interfaces
- Uses `BiFunction<State, ExecutionContext, State>`
- Ideal for simple transformations and lambda expressions

#### NodeBuilder
- Fluent builder for creating nodes
- Auto-generates IDs and names if not provided
- Metadata support
- Validation on build

### 4. Edge Abstraction

#### Edge Interface
- Core contract for connections between nodes
- Methods:
  - `canTraverse()`: Conditional traversal logic
  - `transform()`: State transformation during traversal
  - `priority()`: Priority for edge ordering
- Metadata support
- Source and target node references

#### AbstractEdge
- Base implementation with common functionality
- Proper equals/hashCode based on EdgeId

#### DirectEdge
- Unconditional edge (always traversable)
- Simple source-to-target connections

#### ConditionalEdge
- Edge with conditional traversal logic
- Uses `BiPredicate<State, ExecutionContext>`
- Supports complex routing decisions

### 5. Graph Abstraction

#### Graph Interface
- Container for nodes and edges
- Methods:
  - `nodes()`, `edges()`: Access to graph elements
  - `getNode()`: Lookup by ID
  - `getOutgoingEdges()`, `getIncomingEdges()`: Traversal helpers
  - `entryPoint()`: Starting node for execution
  - `validate()`: Structure validation

#### DefaultGraph
- Immutable implementation
- Validates:
  - At least one node exists
  - Entry point is set and exists
  - All edges reference existing nodes
  - All nodes are reachable from entry point
- Sorts outgoing edges by priority
- Thread-safe after construction

#### GraphBuilder
- Fluent builder for graph construction
- Methods:
  - `addNode()`, `addFunctionalNode()`: Node addition
  - `addEdge()`, `addDirectEdge()`, `addConditionalEdge()`: Edge addition
  - `entryPoint()`: Set starting node
  - `metadata()`: Attach metadata
  - `build()`, `buildAndValidate()`: Construction
- Comprehensive validation
- Auto-generates IDs if not provided

### 6. Context Objects

#### GraphContext
- Immutable context for a graph instance
- Contains GraphId and custom attributes
- Immutable with copy-on-write semantics
- Type-safe attribute access

#### ExecutionContext
- Context for a single execution run
- Contains:
  - Unique execution ID (UUID)
  - Start timestamp
  - Parent GraphContext reference
  - Custom metadata
- Immutable with copy-on-write for metadata
- Tracks execution lifecycle

### 7. Event System

#### GraphEvent (Sealed Interface)
- Type hierarchy for all graph events:
  - `NodeExecutionStarted`: Node begins execution
  - `NodeExecutionCompleted`: Node completes successfully
  - `NodeExecutionFailed`: Node execution fails
  - `EdgeTraversed`: Edge is traversed
  - `StateChanged`: State is modified
  - `GraphExecutionStarted`: Graph execution begins
  - `GraphExecutionCompleted`: Graph execution completes
  - `GraphExecutionFailed`: Graph execution fails
- All events include:
  - Timestamp
  - Execution ID
  - Metadata map
- Sealed interface for compile-time exhaustiveness

#### EventPublisher Interface
- Single method: `publish(GraphEvent)`
- Functional interface for publishing events
- Static `noOp()` factory for testing

#### EventListener Interface
- Single method: `onEvent(GraphEvent)`
- Functional interface for consuming events

#### CompositeEventPublisher
- Multi-listener event publisher
- Thread-safe with CopyOnWriteArrayList
- Methods:
  - `addListener()`, `removeListener()`: Listener management
  - `clearListeners()`: Remove all listeners
  - `publish()`: Publish to all listeners
- Isolates listener exceptions (doesn't propagate)

### 8. Exception Handling

#### GraphValidationException
- Checked exception for graph validation errors
- Clear error messages for debugging
- Used by `Graph.validate()` and `GraphBuilder.buildAndValidate()`

## Key Design Decisions

### 1. Immutability First
- State and most context objects are immutable
- Enables safe concurrent access
- Prevents accidental mutation bugs
- Copy-on-write semantics where needed

### 2. Builder Pattern
- Fluent APIs for all complex constructions
- Validation at build time, not construction time
- Optional fields with sensible defaults
- Auto-generation of IDs where appropriate

### 3. Type Safety
- Generic methods with proper bounds
- Records for value objects
- Sealed interfaces for type hierarchies
- Compile-time safety where possible

### 4. Extensibility Points
- Abstract base classes for Node and Edge
- Interface-based design
- Support for custom implementations
- Metadata maps for additional data

### 5. Event-Driven Architecture
- Comprehensive event system
- Non-blocking event publishing
- Multiple listener support
- Exception isolation

### 6. Lifecycle Management
- Node lifecycle hooks (before/after/error)
- Automatic event publishing
- ExecutionContext tracking
- Timestamp recording

### 7. Thread Safety
- Immutable objects are inherently thread-safe
- StateContainer uses ReadWriteLock
- CompositeEventPublisher uses CopyOnWriteArrayList
- Graph instances immutable after construction

## Testing Coverage

Comprehensive test suite with 81 tests covering:

1. **GraphIdTest** (6 tests): ID creation, validation, equality
2. **StateTest** (11 tests): Immutability, operations, type safety
3. **StateContainerTest** (10 tests): Thread safety, operations, null handling
4. **NodeBuilderTest** (10 tests): Builder pattern, validation, defaults
5. **GraphBuilderTest** (15 tests): Graph construction, validation, edge cases
6. **GraphTest** (9 tests): Graph operations, validation, traversal
7. **NodeExecutionTest** (6 tests): Execution, lifecycle hooks, events
8. **EdgeTest** (6 tests): Edge types, conditions, priorities
9. **EventPublisherTest** (8 tests): Event publishing, listeners, exceptions

All tests pass successfully with `mvn clean verify`.

## Usage Patterns

### Pattern 1: Simple Functional Nodes
```java
Graph graph = GraphBuilder.newGraph()
    .addFunctionalNode(nodeId, "MyNode", 
        (state, ctx) -> state.with("result", "value"))
    .entryPoint(nodeId)
    .build();
```

### Pattern 2: Custom Node with Hooks
```java
Node node = new AbstractNode(id, name, metadata) {
    @Override
    protected State doExecute(State state, ExecutionContext ctx) {
        return state; // Custom logic
    }
    
    @Override
    public void onBeforeExecute(State state, ExecutionContext ctx) {
        // Setup logic
    }
};
```

### Pattern 3: Conditional Routing
```java
GraphBuilder.newGraph()
    .addConditionalEdge(source, target,
        (state, ctx) -> state.has("condition"))
    .build();
```

### Pattern 4: Event Monitoring
```java
CompositeEventPublisher publisher = new CompositeEventPublisher();
publisher.addListener(event -> {
    // Handle events
});
```

## Future Enhancements

Potential areas for future development:
1. Async/reactive execution support
2. Graph serialization/deserialization
3. Visual graph representation
4. Performance metrics and profiling
5. Distributed execution support
6. State persistence integration
7. Advanced validation rules
8. Graph optimization algorithms

## Dependencies

The core module has minimal dependencies:
- Jackson (for potential serialization)
- SLF4J (for logging)
- JUnit 5 (testing)
- Mockito (testing)

No runtime dependencies on heavyweight frameworks.
