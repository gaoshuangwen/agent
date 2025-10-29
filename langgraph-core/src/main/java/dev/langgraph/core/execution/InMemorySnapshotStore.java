package dev.langgraph.core.execution;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class InMemorySnapshotStore implements SnapshotStore {
    
    private final Map<String, List<StateSnapshot>> snapshots = new ConcurrentHashMap<>();

    @Override
    public void save(StateSnapshot snapshot) {
        snapshots.computeIfAbsent(snapshot.executionId(), k -> new ArrayList<>()).add(snapshot);
    }

    @Override
    public Optional<StateSnapshot> getLatest(String executionId) {
        List<StateSnapshot> execSnapshots = snapshots.get(executionId);
        if (execSnapshots == null || execSnapshots.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(execSnapshots.get(execSnapshots.size() - 1));
    }

    @Override
    public List<StateSnapshot> getAll(String executionId) {
        return snapshots.getOrDefault(executionId, List.of()).stream()
                .sorted(Comparator.comparing(StateSnapshot::timestamp))
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String executionId) {
        snapshots.remove(executionId);
    }
}
