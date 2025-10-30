package dev.langgraph.core.human;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HumanInTheLoopIntegrationTest {

    private HumanTaskManager taskManager;
    private InMemoryHumanTaskStore taskStore;
    private GraphContext graphContext;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        taskStore = new InMemoryHumanTaskStore();
        taskManager = new HumanTaskManager(taskStore, EventPublisher.noOp());
        graphContext = GraphContext.of(GraphId.of("test-graph"));
        executionContext = ExecutionContext.create(graphContext);
    }

    @Test
    void shouldCreatePendingTaskOnFirstExecution() {
        HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                        NodeId.of("approval"), "Approval", taskManager)
                .prompt("Approve deletion?")
                .timeout(Duration.ofMinutes(5))
                .build();

        State initialState = State.empty().with("action", "delete");

        assertThrows(HumanTaskPendingException.class, () -> {
            approvalNode.execute(initialState, executionContext, EventPublisher.noOp());
        });

        assertEquals(1, taskManager.getPendingTasks().size());
        HumanTask task = taskManager.getPendingTasks().get(0);
        assertEquals(HumanTaskType.APPROVAL, task.type());
        assertEquals("Approve deletion?", task.prompt());
    }

    @Test
    void shouldCompleteApprovalWhenResponded() throws Exception {
        HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                        NodeId.of("approval"), "Approval", taskManager)
                .prompt("Approve?")
                .build();

        State initialState = State.empty();

        try {
            approvalNode.execute(initialState, executionContext, EventPublisher.noOp());
            fail("Should have thrown HumanTaskPendingException");
        } catch (HumanTaskPendingException e) {
            HumanTask task = e.getTask();
            taskManager.completeTask(task.taskId(), Map.of("approved", true, "comments", "OK"));
        }

        State resultState = approvalNode.execute(initialState, executionContext, EventPublisher.noOp());

        assertEquals(true, resultState.get("__human_approved__", false));
        assertEquals("OK", resultState.get("__human_comments__", ""));
    }

    @Test
    void shouldCollectHumanInput() throws Exception {
        HumanInputNode inputNode = HumanInputNode.builder(
                        NodeId.of("input"), "Input", taskManager)
                .prompt("Enter project name")
                .outputKey("projectName")
                .build();

        State initialState = State.empty();

        try {
            inputNode.execute(initialState, executionContext, EventPublisher.noOp());
            fail("Should have thrown HumanTaskPendingException");
        } catch (HumanTaskPendingException e) {
            HumanTask task = e.getTask();
            taskManager.completeTask(task.taskId(), Map.of("input", "MyProject"));
        }

        State resultState = inputNode.execute(initialState, executionContext, EventPublisher.noOp());

        assertEquals("MyProject", resultState.get("projectName", ""));
    }

    @Test
    void shouldHandleTimeoutWithDefaultResponse() throws Exception {
        TimeoutStrategy strategy = TimeoutStrategy.continueWithDefault(
                Map.of("approved", false, "reason", "timeout")
        );

        HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                        NodeId.of("approval"), "Approval", taskManager)
                .prompt("Approve?")
                .timeout(Duration.ofMillis(10))
                .timeoutStrategy(strategy)
                .build();

        State initialState = State.empty();

        try {
            approvalNode.execute(initialState, executionContext, EventPublisher.noOp());
            fail("Should have thrown HumanTaskPendingException");
        } catch (HumanTaskPendingException e) {
            // Task created, now wait for timeout
        }

        Thread.sleep(20);

        State resultState = approvalNode.execute(initialState, executionContext, EventPublisher.noOp());

        assertTrue(resultState.get("__human_task_timed_out__", false));
        assertNotNull(resultState.get("__human_response__"));
    }

    @Test
    void shouldRejectDoubleApproval() throws Exception {
        HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                        NodeId.of("approval"), "Approval", taskManager)
                .prompt("Approve?")
                .build();

        State initialState = State.empty();

        try {
            approvalNode.execute(initialState, executionContext, EventPublisher.noOp());
        } catch (HumanTaskPendingException e) {
            HumanTask task = e.getTask();
            taskManager.completeTask(task.taskId(), Map.of("approved", true));
            
            assertThrows(IllegalStateException.class, () -> {
                taskManager.completeTask(task.taskId(), Map.of("approved", false));
            });
        }
    }

    @Test
    void shouldHandleMultipleNodesWithDifferentTasks() {
        HumanApprovalNode approval1 = HumanApprovalNode.builder(
                        NodeId.of("approval1"), "Approval1", taskManager)
                .prompt("Approve step 1?")
                .build();

        HumanApprovalNode approval2 = HumanApprovalNode.builder(
                        NodeId.of("approval2"), "Approval2", taskManager)
                .prompt("Approve step 2?")
                .build();

        State state = State.empty();

        assertThrows(HumanTaskPendingException.class, () -> {
            approval1.execute(state, executionContext, EventPublisher.noOp());
        });

        assertThrows(HumanTaskPendingException.class, () -> {
            approval2.execute(state, executionContext, EventPublisher.noOp());
        });

        assertEquals(2, taskManager.getPendingTasks().size());
        
        HumanTask task1 = taskManager.getPendingTasks().stream()
                .filter(t -> t.nodeId().equals("approval1"))
                .findFirst().orElseThrow();
        
        HumanTask task2 = taskManager.getPendingTasks().stream()
                .filter(t -> t.nodeId().equals("approval2"))
                .findFirst().orElseThrow();

        assertNotEquals(task1.taskId(), task2.taskId());
        assertEquals("Approve step 1?", task1.prompt());
        assertEquals("Approve step 2?", task2.prompt());
    }

    @Test
    void shouldCancelTask() {
        HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                        NodeId.of("approval"), "Approval", taskManager)
                .prompt("Approve?")
                .build();

        try {
            approvalNode.execute(State.empty(), executionContext, EventPublisher.noOp());
        } catch (HumanTaskPendingException e) {
            HumanTask task = e.getTask();
            taskManager.cancelTask(task.taskId());

            assertEquals(HumanTaskStatus.CANCELLED, 
                    taskManager.getTask(task.taskId()).orElseThrow().status());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void shouldEscalateTask() {
        HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                        NodeId.of("approval"), "Approval", taskManager)
                .prompt("Approve?")
                .build();

        try {
            approvalNode.execute(State.empty(), executionContext, EventPublisher.noOp());
        } catch (HumanTaskPendingException e) {
            HumanTask task = e.getTask();
            taskManager.escalateTask(task.taskId());

            assertEquals(HumanTaskStatus.ESCALATED, 
                    taskManager.getTask(task.taskId()).orElseThrow().status());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
