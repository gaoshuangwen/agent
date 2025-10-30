package dev.langgraph.core.human;

import dev.langgraph.core.event.EventPublisher;
import dev.langgraph.core.event.GraphEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class HumanTaskManager {
    
    private final HumanTaskStore store;
    private final EventPublisher eventPublisher;

    public HumanTaskManager(HumanTaskStore store, EventPublisher eventPublisher) {
        this.store = Objects.requireNonNull(store, "HumanTaskStore cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "EventPublisher cannot be null");
    }

    public HumanTask createTask(String executionId, String nodeId, HumanTaskType type,
                                String prompt, Map<String, Object> context, Duration timeout) {
        HumanTask task = HumanTask.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .type(type)
                .prompt(prompt)
                .context(context)
                .timeout(timeout)
                .build();

        store.save(task);

        eventPublisher.publish(new GraphEvent.HumanTaskCreated(
                executionId,
                task.taskId(),
                nodeId,
                type.name(),
                Instant.now(),
                Map.of("prompt", prompt, "timeout", timeout != null ? timeout.toString() : "none")
        ));

        return task;
    }

    public void completeTask(String taskId, Map<String, Object> response) {
        HumanTask task = store.load(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.isPending()) {
            throw new IllegalStateException("Task is not in PENDING status: " + taskId);
        }

        if (task.isExpired()) {
            HumanTask expiredTask = task.withStatus(HumanTaskStatus.EXPIRED);
            store.save(expiredTask);
            throw new HumanTaskTimeoutException("Task has expired: " + taskId);
        }

        HumanTask completedTask = task.withResponse(response);
        store.save(completedTask);

        eventPublisher.publish(new GraphEvent.HumanTaskCompleted(
                task.executionId(),
                taskId,
                task.nodeId(),
                Instant.now(),
                Map.of("response", response)
        ));
    }

    public void cancelTask(String taskId) {
        HumanTask task = store.load(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        HumanTask cancelledTask = task.withStatus(HumanTaskStatus.CANCELLED);
        store.save(cancelledTask);

        eventPublisher.publish(new GraphEvent.HumanTaskCancelled(
                task.executionId(),
                taskId,
                task.nodeId(),
                Instant.now(),
                Map.of()
        ));
    }

    public void escalateTask(String taskId) {
        HumanTask task = store.load(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        HumanTask escalatedTask = task.withStatus(HumanTaskStatus.ESCALATED);
        store.save(escalatedTask);

        eventPublisher.publish(new GraphEvent.HumanTaskEscalated(
                task.executionId(),
                taskId,
                task.nodeId(),
                Instant.now(),
                Map.of()
        ));
    }

    public Optional<HumanTask> getTask(String taskId) {
        return store.load(taskId);
    }

    public List<HumanTask> getPendingTasks() {
        return store.listPendingTasks();
    }

    public List<HumanTask> getPendingTasksForExecution(String executionId) {
        return store.listByExecutionId(executionId).stream()
                .filter(HumanTask::isPending)
                .toList();
    }

    public List<HumanTask> getExpiredTasks() {
        return store.listExpiredTasks();
    }

    public void processExpiredTasks(TimeoutStrategy strategy, 
                                    dev.langgraph.core.ExecutionContext context,
                                    dev.langgraph.core.State state) {
        List<HumanTask> expired = getExpiredTasks();
        for (HumanTask task : expired) {
            HumanTask expiredTask = task.withStatus(HumanTaskStatus.EXPIRED);
            store.save(expiredTask);

            eventPublisher.publish(new GraphEvent.HumanTaskExpired(
                    task.executionId(),
                    task.taskId(),
                    task.nodeId(),
                    Instant.now(),
                    Map.of()
            ));
        }
    }

    public boolean hasPendingTasks(String executionId) {
        return !getPendingTasksForExecution(executionId).isEmpty();
    }

    public void deleteTasksForExecution(String executionId) {
        store.deleteByExecutionId(executionId);
    }

    public HumanTaskStore getStore() {
        return store;
    }
}
