package dev.langgraph.core.human;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HumanTaskTest {

    @Test
    void shouldCreateHumanTask() {
        HumanTask task = HumanTask.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .type(HumanTaskType.APPROVAL)
                .prompt("Approve this action?")
                .context(Map.of("action", "delete"))
                .build();

        assertNotNull(task.taskId());
        assertEquals("exec-1", task.executionId());
        assertEquals("node-1", task.nodeId());
        assertEquals(HumanTaskType.APPROVAL, task.type());
        assertEquals("Approve this action?", task.prompt());
        assertEquals(HumanTaskStatus.PENDING, task.status());
        assertTrue(task.isPending());
        assertFalse(task.isCompleted());
    }

    @Test
    void shouldSetTimeout() {
        HumanTask task = HumanTask.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .type(HumanTaskType.INPUT)
                .prompt("Enter value")
                .timeout(Duration.ofMinutes(5))
                .build();

        assertNotNull(task.expiresAt());
        assertTrue(task.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void shouldCompleteTask() {
        HumanTask task = HumanTask.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .type(HumanTaskType.APPROVAL)
                .prompt("Approve?")
                .build();

        Map<String, Object> response = Map.of("approved", true, "comments", "Looks good");
        HumanTask completed = task.withResponse(response);

        assertTrue(completed.isCompleted());
        assertEquals(true, completed.response().get("approved"));
        assertEquals("Looks good", completed.response().get("comments"));
        assertNotNull(completed.completedAt());
    }

    @Test
    void shouldDetectExpiredTask() throws InterruptedException {
        HumanTask task = HumanTask.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .type(HumanTaskType.APPROVAL)
                .prompt("Approve?")
                .timeout(Duration.ofMillis(10))
                .build();

        Thread.sleep(20);

        assertTrue(task.isExpired());
    }

    @Test
    void shouldChangeStatus() {
        HumanTask task = HumanTask.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .type(HumanTaskType.APPROVAL)
                .prompt("Approve?")
                .build();

        HumanTask cancelled = task.withStatus(HumanTaskStatus.CANCELLED);
        assertEquals(HumanTaskStatus.CANCELLED, cancelled.status());
        
        HumanTask escalated = task.withStatus(HumanTaskStatus.ESCALATED);
        assertEquals(HumanTaskStatus.ESCALATED, escalated.status());
    }

    @Test
    void shouldAddContext() {
        HumanTask task = HumanTask.builder()
                .executionId("exec-1")
                .nodeId("node-1")
                .type(HumanTaskType.REVIEW)
                .prompt("Review document")
                .addContext("documentId", "doc-123")
                .addContext("author", "John Doe")
                .build();

        assertEquals("doc-123", task.context().get("documentId"));
        assertEquals("John Doe", task.context().get("author"));
    }
}
