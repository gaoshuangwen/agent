# LangGraph Persistence

Persistence layer for LangGraph providing checkpoint management, state serialization, and resumable execution.

## Features

- **Checkpoint Management**: Save and restore execution state at any point
- **Multiple Storage Backends**: In-memory and file-based storage included
- **Resumable Execution**: Resume interrupted workflows from the last checkpoint
- **State Serialization**: Jackson-based JSON serialization for complex state
- **Extensible Architecture**: Easy to add custom storage backends

## Storage Backends

### InMemoryCheckpointStore

Thread-safe in-memory storage for development and testing:

```java
CheckpointStore store = new InMemoryCheckpointStore();
CheckpointManager manager = new CheckpointManager(store);
```

### FileBasedCheckpointStore

Persistent file-based storage using JSON:

```java
Path storageDir = Paths.get("/var/langgraph/checkpoints");
CheckpointStore store = new FileBasedCheckpointStore(storageDir);
CheckpointManager manager = new CheckpointManager(store);
```

## Usage

### Creating Checkpoints

```java
CheckpointManager manager = new CheckpointManager(store);

// Create checkpoint
Map<String, Object> state = Map.of(
        "step", 1,
        "data", "processing"
);

Checkpoint checkpoint = manager.createCheckpoint(
        "graph-id",
        "execution-id",
        state,
        "current-node-id"
);
```

### Loading Checkpoints

```java
// Load specific checkpoint
Optional<Checkpoint> checkpoint = manager.loadCheckpoint("checkpoint-id");

// Get latest checkpoint for execution
Optional<Checkpoint> latest = manager.getLatestCheckpoint("execution-id");

// Get checkpoint history
List<Checkpoint> history = manager.getCheckpointHistory("execution-id");
```

### Resuming Execution

```java
// Check if execution can be resumed
if (manager.canResume("execution-id")) {
    Checkpoint resumePoint = manager.getLatestCheckpoint("execution-id")
            .orElseThrow();
    
    // Resume from checkpoint
    Map<String, Object> state = resumePoint.state();
    String currentNode = resumePoint.currentNodeId();
    
    // Continue execution...
}
```

### Checkpoint Cleanup

```java
// Delete specific checkpoint
manager.deleteCheckpoint("checkpoint-id");

// Delete all checkpoints for an execution
manager.deleteExecutionCheckpoints("execution-id");
```

## Checkpoint Structure

```java
Checkpoint checkpoint = Checkpoint.builder()
        .checkpointId("cp-123")                  // Unique ID (auto-generated if not provided)
        .graphId("workflow-graph")               // Graph identifier
        .executionId("exec-456")                 // Execution identifier
        .state(Map.of("key", "value"))          // Current state
        .currentNodeId("processing-node")        // Current execution position
        .timestamp(Instant.now())                // Timestamp (auto-generated)
        .metadata(Map.of("phase", "init"))      // Optional metadata
        .build();
```

## Serialization

Uses Jackson for JSON serialization with support for:
- Primitive types (String, Number, Boolean)
- Collections (List, Map, Set)
- Java time types (Instant, LocalDateTime, etc.)
- Nested structures
- Custom objects (with proper Jackson annotations)

```java
StateSerializer serializer = new JacksonStateSerializer();

// Serialize
byte[] data = serializer.serialize(state);
String json = serializer.serializeToString(state);

// Deserialize
Map state = serializer.deserialize(data, Map.class);
Map state = serializer.deserializeFromString(json, Map.class);
```

## Custom Storage Backend

Implement the `CheckpointStore` interface:

```java
public class DatabaseCheckpointStore implements CheckpointStore {
    
    @Override
    public void save(Checkpoint checkpoint) throws PersistenceException {
        // Store in database
    }
    
    @Override
    public Optional<Checkpoint> load(String checkpointId) throws PersistenceException {
        // Load from database
        return Optional.empty();
    }
    
    // Implement other methods...
}
```

## Integration with Execution Engine

The persistence layer is designed to integrate with the LangGraph execution engine:

```java
// Create checkpoint during execution
ExecutionConfig config = ExecutionConfig.builder()
        .enableSnapshots(true)
        .snapshotStore(new YourSnapshotStore())
        .build();

StateGraph stateGraph = StateGraph.of(graph, config);

// Checkpoints are automatically created at execution boundaries
ExecutionResult result = stateGraph.execute(initialState);
```

## Best Practices

### 1. Choose Appropriate Storage

- **InMemoryCheckpointStore**: Development, testing, short-lived processes
- **FileBasedCheckpointStore**: Production, long-running workflows, single-node deployments
- **Custom Database Backend**: Production, distributed systems, high availability

### 2. Checkpoint Strategically

Don't checkpoint after every node - focus on:
- Before expensive operations
- After significant state changes
- Before potentially failing operations
- At natural workflow boundaries

### 3. Manage Checkpoint Lifecycle

```java
// Clean up old checkpoints
manager.deleteExecutionCheckpoints("completed-execution-id");

// Or implement automatic cleanup based on age
checkpoints.stream()
        .filter(cp -> cp.timestamp().isBefore(cutoffTime))
        .forEach(cp -> manager.deleteCheckpoint(cp.checkpointId()));
```

### 4. Handle Serialization Carefully

Ensure all state objects are serializable:
- Use Jackson annotations for custom objects
- Avoid storing non-serializable resources (connections, threads, etc.)
- Test serialization/deserialization of complex states

### 5. Monitor Storage Usage

```java
// For InMemoryCheckpointStore
InMemoryCheckpointStore store = new InMemoryCheckpointStore();
int checkpointCount = store.size();

// For FileBasedCheckpointStore
long storageSize = Files.walk(storageDir)
        .filter(Files::isRegularFile)
        .mapToLong(p -> p.toFile().length())
        .sum();
```

## Configuration

### File-Based Storage

```java
Path storageDir = Paths.get("/var/langgraph/checkpoints");
ObjectMapper customMapper = new ObjectMapper()
        .configure(SerializationFeature.INDENT_OUTPUT, true);

FileBasedCheckpointStore store = new FileBasedCheckpointStore(
        storageDir, 
        customMapper
);
```

### Custom Serializer

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new MyCustomModule());

StateSerializer serializer = new JacksonStateSerializer(mapper);
CheckpointManager manager = new CheckpointManager(store, serializer);
```

## Error Handling

```java
try {
    Checkpoint checkpoint = manager.createCheckpoint(
            graphId, executionId, state, nodeId
    );
} catch (PersistenceException e) {
    // Handle storage errors
    logger.error("Failed to create checkpoint", e);
}

try {
    Optional<Checkpoint> checkpoint = manager.loadCheckpoint(checkpointId);
} catch (PersistenceException e) {
    // Handle load errors
    logger.error("Failed to load checkpoint", e);
}
```

## Testing

Run persistence tests:

```bash
mvn test -pl langgraph-persistence
```

The test suite includes:
- Checkpoint creation and retrieval
- In-memory store operations
- File-based persistence
- Serialization/deserialization
- Recovery scenarios
- Concurrent access
- State consistency

## Thread Safety

- `InMemoryCheckpointStore`: Thread-safe with `ConcurrentHashMap`
- `FileBasedCheckpointStore`: File-system level safety
- `CheckpointManager`: Thread-safe when using thread-safe stores
- `Checkpoint`: Immutable and thread-safe

## Performance Considerations

### In-Memory Storage
- Fast read/write operations
- Limited by available memory
- Lost on process restart

### File-Based Storage
- Slower than in-memory but persistent
- I/O bound performance
- Storage space requirements

### Optimization Tips
- Batch checkpoint operations when possible
- Use appropriate checkpoint frequency
- Implement checkpoint compression for large states
- Consider async checkpoint writing for performance-critical paths

## Examples

See integration tests for complete examples:
- `CheckpointRecoveryIntegrationTest`: End-to-end recovery scenarios
- `CheckpointManagerTest`: Manager operations
- `FileBasedCheckpointStoreTest`: File persistence
