package dev.langgraph.core.execution;

import java.util.List;
import java.util.Optional;

final class NoOpSnapshotStore implements SnapshotStore {
    
    @Override
    public void save(StateSnapshot snapshot) {
    }

    @Override
    public Optional<StateSnapshot> getLatest(String executionId) {
        return Optional.empty();
    }

    @Override
    public List<StateSnapshot> getAll(String executionId) {
        return List.of();
    }

    @Override
    public void clear(String executionId) {
    }
}
