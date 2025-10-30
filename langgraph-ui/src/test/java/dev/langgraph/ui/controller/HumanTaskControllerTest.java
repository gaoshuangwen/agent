package dev.langgraph.ui.controller;

import dev.langgraph.core.human.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HumanTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HumanTaskManager humanTaskManager;

    private String testTaskId;

    @BeforeEach
    void setUp() {
        HumanTask task = humanTaskManager.createTask(
                "test-execution-1",
                "approval-node",
                HumanTaskType.APPROVAL,
                "Please approve this action",
                Map.of(),
                Duration.ofMinutes(5)
        );

        testTaskId = task.taskId();
    }

    @Test
    void testGetPendingTasks() throws Exception {
        mockMvc.perform(get("/api/tasks/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].taskId").value(testTaskId))
                .andExpect(jsonPath("$[0].prompt").value("Please approve this action"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void testGetTasksForExecution() throws Exception {
        mockMvc.perform(get("/api/tasks/execution/test-execution-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].executionId").value("test-execution-1"));
    }

    @Test
    void testGetTask() throws Exception {
        mockMvc.perform(get("/api/tasks/" + testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(testTaskId))
                .andExpect(jsonPath("$.executionId").value("test-execution-1"))
                .andExpect(jsonPath("$.type").value("APPROVAL"));
    }

    @Test
    void testGetNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/tasks/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testApproveTask() throws Exception {
        mockMvc.perform(post("/api/tasks/" + testTaskId + "/approve"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/" + testTaskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testRejectTask() throws Exception {
        HumanTask newTask = humanTaskManager.createTask(
                "test-execution-2",
                "approval-node",
                HumanTaskType.APPROVAL,
                "Another approval",
                Map.of(),
                Duration.ofMinutes(5)
        );

        mockMvc.perform(post("/api/tasks/" + newTask.taskId() + "/reject"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/" + newTask.taskId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testRespondToTask() throws Exception {
        HumanTask inputTask = humanTaskManager.createTask(
                "test-execution-3",
                "input-node",
                HumanTaskType.INPUT,
                "Enter your name",
                Map.of(),
                Duration.ofMinutes(5)
        );

        mockMvc.perform(post("/api/tasks/" + inputTask.taskId() + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "response": {
                                        "name": "John Doe"
                                    }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/" + inputTask.taskId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.response.name").value("John Doe"));
    }
}
