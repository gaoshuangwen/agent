package dev.langgraph.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CheckpointManager {
    
    private final CheckpointStore store;
    private final StateSerializer serializer;

    public CheckpointManager(CheckpointStore store) {
        this(store, new JacksonStateSerializer());
    }

    public CheckpointManager(CheckpointStore store, StateSerializer serializer) {
        this.store = Objects.requireNonNull(store, "CheckpointStore cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "StateSerializer cannot be null");
    }

    public Checkpoint createCheckpoint(String graphId, String executionId, 
                                      Map<String, Object> state, String currentNodeId) 
            throws PersistenceException {
        Checkpoint checkpoint = Checkpoint.builder()
                .graphId(graphId)
                .executionId(executionId)
                .state(state)
                .currentNodeId(currentNodeId)
                .timestamp(Instant.now())
                .build();
        
        store.save(checkpoint);
        return checkpoint;
    }

    public Checkpoint createCheckpoint(String graphId, String executionId, 
                                      Map<String, Object> state, String currentNodeId,
                                      Map<String, Object> metadata) 
            throws PersistenceException {
        Checkpoint checkpoint = Checkpoint.builder()
                .graphId(graphId)
                .executionId(executionId)
                .state(state)
                .currentNodeId(currentNodeId)
                .timestamp(Instant.now())
                .metadata(metadata)
                .build();
        
        store.save(checkpoint);
        return checkpoint;
    }

    public Optional<Checkpoint> loadCheckpoint(String checkpointId) throws PersistenceException {
        return store.load(checkpointId);
    }

    public Optional<Checkpoint> getLatestCheckpoint(String executionId) throws PersistenceException {
        return store.getLatestByExecutionId(executionId);
    }

    public List<Checkpoint> getCheckpointHistory(String executionId) throws PersistenceException {
        return store.listByExecutionId(executionId);
    }

    public void deleteCheckpoint(String checkpointId) throws PersistenceException {
        store.delete(checkpointId);
    }

    public void deleteExecutionCheckpoints(String executionId) throws PersistenceException {
        store.deleteByExecutionId(executionId);
    }

    public boolean canResume(String executionId) throws PersistenceException {
        return store.getLatestByExecutionId(executionId).isPresent();
    }

    public CheckpointStore getStore() {
        return store;
    }

    public StateSerializer getSerializer() {
        return serializer;
    }
}
