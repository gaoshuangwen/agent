package dev.langgraph.core.human;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class InMemoryHumanTaskStore implements HumanTaskStore {
    
    private final Map<String, HumanTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, List<String>> executionIndex = new ConcurrentHashMap<>();

    @Override
    public void save(HumanTask task) {
        tasks.put(task.taskId(), task);
        executionIndex.computeIfAbsent(task.executionId(), k -> new ArrayList<>())
                .add(task.taskId());
    }

    @Override
    public Optional<HumanTask> load(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public List<HumanTask> listByExecutionId(String executionId) {
        List<String> taskIds = executionIndex.getOrDefault(executionId, List.of());
        return taskIds.stream()
                .map(tasks::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HumanTask::createdAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<HumanTask> listPendingTasks() {
        return tasks.values().stream()
                .filter(HumanTask::isPending)
                .sorted(Comparator.comparing(HumanTask::createdAt))
                .collect(Collectors.toList());
    }

    @Override
    public List<HumanTask> listExpiredTasks() {
        Instant now = Instant.now();
        return tasks.values().stream()
                .filter(task -> task.expiresAt() != null && now.isAfter(task.expiresAt()))
                .filter(task -> task.status() == HumanTaskStatus.PENDING)
                .sorted(Comparator.comparing(HumanTask::expiresAt))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String taskId) {
        HumanTask task = tasks.remove(taskId);
        if (task != null) {
            List<String> execList = executionIndex.get(task.executionId());
            if (execList != null) {
                execList.remove(taskId);
            }
        }
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        List<String> taskIds = new ArrayList<>(
                executionIndex.getOrDefault(executionId, List.of()));
        for (String taskId : taskIds) {
            delete(taskId);
        }
        executionIndex.remove(executionId);
    }

    @Override
    public void clear() {
        tasks.clear();
        executionIndex.clear();
    }

    @Override
    public boolean exists(String taskId) {
        return tasks.containsKey(taskId);
    }

    public int size() {
        return tasks.size();
    }
}
