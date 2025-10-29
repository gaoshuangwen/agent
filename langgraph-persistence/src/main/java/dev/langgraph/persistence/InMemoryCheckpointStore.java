package dev.langgraph.persistence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryCheckpointStore implements CheckpointStore {
    
    private final Map<String, Checkpoint> checkpoints = new ConcurrentHashMap<>();
    private final Map<String, List<String>> executionIndex = new ConcurrentHashMap<>();
    private final Map<String, List<String>> graphIndex = new ConcurrentHashMap<>();

    @Override
    public void save(Checkpoint checkpoint) {
        checkpoints.put(checkpoint.checkpointId(), checkpoint);
        
        executionIndex.computeIfAbsent(checkpoint.executionId(), k -> new ArrayList<>())
                .add(checkpoint.checkpointId());
        
        graphIndex.computeIfAbsent(checkpoint.graphId(), k -> new ArrayList<>())
                .add(checkpoint.checkpointId());
    }

    @Override
    public Optional<Checkpoint> load(String checkpointId) {
        return Optional.ofNullable(checkpoints.get(checkpointId));
    }

    @Override
    public List<Checkpoint> listByExecutionId(String executionId) {
        List<String> checkpointIds = executionIndex.getOrDefault(executionId, List.of());
        return checkpointIds.stream()
                .map(checkpoints::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Checkpoint::timestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<Checkpoint> listByGraphId(String graphId) {
        List<String> checkpointIds = graphIndex.getOrDefault(graphId, List.of());
        return checkpointIds.stream()
                .map(checkpoints::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Checkpoint::timestamp))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Checkpoint> getLatestByExecutionId(String executionId) {
        return listByExecutionId(executionId).stream()
                .max(Comparator.comparing(Checkpoint::timestamp));
    }

    @Override
    public void delete(String checkpointId) {
        Checkpoint checkpoint = checkpoints.remove(checkpointId);
        if (checkpoint != null) {
            List<String> execList = executionIndex.get(checkpoint.executionId());
            if (execList != null) {
                execList.remove(checkpointId);
            }
            
            List<String> graphList = graphIndex.get(checkpoint.graphId());
            if (graphList != null) {
                graphList.remove(checkpointId);
            }
        }
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        List<String> checkpointIds = new ArrayList<>(
                executionIndex.getOrDefault(executionId, List.of()));
        for (String checkpointId : checkpointIds) {
            delete(checkpointId);
        }
        executionIndex.remove(executionId);
    }

    @Override
    public void clear() {
        checkpoints.clear();
        executionIndex.clear();
        graphIndex.clear();
    }

    @Override
    public boolean exists(String checkpointId) {
        return checkpoints.containsKey(checkpointId);
    }

    public int size() {
        return checkpoints.size();
    }
}
