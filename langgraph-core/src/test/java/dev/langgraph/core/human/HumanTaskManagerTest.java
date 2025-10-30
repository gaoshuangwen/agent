package dev.langgraph.core.human;

import dev.langgraph.core.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HumanTaskManagerTest {

    private HumanTaskManager manager;
    private InMemoryHumanTaskStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryHumanTaskStore();
        manager = new HumanTaskManager(store, EventPublisher.noOp());
    }

    @Test
    void shouldCreateTask() {
        HumanTask task = manager.createTask(
                "exec-1",
                "node-1",
                HumanTaskType.APPROVAL,
                "Approve this action?",
                Map.of("action", "delete"),
                Duration.ofMinutes(5)
        );

        assertNotNull(task.taskId());
        assertEquals("exec-1", task.executionId());
        assertEquals(HumanTaskType.APPROVAL, task.type());
        assertTrue(store.exists(task.taskId()));
    }

    @Test
    void shouldCompleteTask() {
        HumanTask task = manager.createTask(
                "exec-1",
                "node-1",
                HumanTaskType.APPROVAL,
                "Approve?",
                Map.of(),
                null
        );

        Map<String, Object> response = Map.of("approved", true);
        manager.completeTask(task.taskId(), response);

        HumanTask completed = manager.getTask(task.taskId()).orElseThrow();
        assertTrue(completed.isCompleted());
        assertEquals(true, completed.response().get("approved"));
    }

    @Test
    void shouldThrowOnCompletingNonPendingTask() {
        HumanTask task = manager.createTask(
                "exec-1",
                "node-1",
                HumanTaskType.APPROVAL,
                "Approve?",
                Map.of(),
                null
        );

        manager.completeTask(task.taskId(), Map.of("approved", true));

        assertThrows(IllegalStateException.class, () -> {
            manager.completeTask(task.taskId(), Map.of("approved", false));
        });
    }

    @Test
    void shouldCancelTask() {
        HumanTask task = manager.createTask(
                "exec-1",
                "node-1",
                HumanTaskType.INPUT,
                "Enter value",
                Map.of(),
                null
        );

        manager.cancelTask(task.taskId());

        HumanTask cancelled = manager.getTask(task.taskId()).orElseThrow();
        assertEquals(HumanTaskStatus.CANCELLED, cancelled.status());
    }

    @Test
    void shouldEscalateTask() {
        HumanTask task = manager.createTask(
                "exec-1",
                "node-1",
                HumanTaskType.REVIEW,
                "Review document",
                Map.of(),
                null
        );

        manager.escalateTask(task.taskId());

        HumanTask escalated = manager.getTask(task.taskId()).orElseThrow();
        assertEquals(HumanTaskStatus.ESCALATED, escalated.status());
    }

    @Test
    void shouldListPendingTasks() {
        manager.createTask("exec-1", "node-1", HumanTaskType.APPROVAL, "Task 1", Map.of(), null);
        manager.createTask("exec-1", "node-2", HumanTaskType.INPUT, "Task 2", Map.of(), null);
        HumanTask task3 = manager.createTask("exec-1", "node-3", HumanTaskType.REVIEW, "Task 3", Map.of(), null);

        manager.completeTask(task3.taskId(), Map.of("reviewed", true));

        List<HumanTask> pending = manager.getPendingTasks();
        assertEquals(2, pending.size());
    }

    @Test
    void shouldListPendingTasksForExecution() {
        manager.createTask("exec-1", "node-1", HumanTaskType.APPROVAL, "Task 1", Map.of(), null);
        manager.createTask("exec-1", "node-2", HumanTaskType.INPUT, "Task 2", Map.of(), null);
        manager.createTask("exec-2", "node-1", HumanTaskType.APPROVAL, "Task 3", Map.of(), null);

        List<HumanTask> pending = manager.getPendingTasksForExecution("exec-1");
        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(t -> t.executionId().equals("exec-1")));
    }

    @Test
    void shouldCheckIfHasPendingTasks() {
        assertFalse(manager.hasPendingTasks("exec-1"));

        manager.createTask("exec-1", "node-1", HumanTaskType.APPROVAL, "Task", Map.of(), null);

        assertTrue(manager.hasPendingTasks("exec-1"));
    }

    @Test
    void shouldDeleteTasksForExecution() {
        manager.createTask("exec-1", "node-1", HumanTaskType.APPROVAL, "Task 1", Map.of(), null);
        manager.createTask("exec-1", "node-2", HumanTaskType.INPUT, "Task 2", Map.of(), null);
        manager.createTask("exec-2", "node-1", HumanTaskType.APPROVAL, "Task 3", Map.of(), null);

        manager.deleteTasksForExecution("exec-1");

        assertFalse(manager.hasPendingTasks("exec-1"));
        assertTrue(manager.hasPendingTasks("exec-2"));
    }
}
