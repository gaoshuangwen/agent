package dev.langgraph.core.execution;

import java.util.List;
import java.util.Optional;

public interface SnapshotStore {
    
    void save(StateSnapshot snapshot);
    
    Optional<StateSnapshot> getLatest(String executionId);
    
    List<StateSnapshot> getAll(String executionId);
    
    void clear(String executionId);
    
    static SnapshotStore inMemory() {
        return new InMemorySnapshotStore();
    }
    
    static SnapshotStore noOp() {
        return new NoOpSnapshotStore();
    }
}
