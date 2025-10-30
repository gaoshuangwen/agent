package com.langgraph.ui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.langgraph.ui.dto.StartExecutionRequest;
import com.langgraph.ui.model.ExecutionStatus;
import com.langgraph.ui.model.GraphExecution;
import com.langgraph.ui.service.GraphExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraphExecutionController.class)
class GraphExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GraphExecutionService executionService;

    @Test
    void shouldStartExecution() throws Exception {
        StartExecutionRequest request = StartExecutionRequest.builder()
                .graphId("test-graph")
                .input(Map.of("key", "value"))
                .build();

        GraphExecution execution = GraphExecution.builder()
                .executionId("exec-123")
                .graphId("test-graph")
                .status(ExecutionStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .input(Map.of("key", "value"))
                .nodeExecutions(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        when(executionService.startExecution(anyString(), any())).thenReturn(execution);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void shouldGetExecution() throws Exception {
        GraphExecution execution = GraphExecution.builder()
                .executionId("exec-123")
                .graphId("test-graph")
                .status(ExecutionStatus.COMPLETED)
                .startTime(LocalDateTime.now())
                .nodeExecutions(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        when(executionService.getExecution("exec-123")).thenReturn(Optional.of(execution));

        mockMvc.perform(get("/api/executions/exec-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentExecution() throws Exception {
        when(executionService.getExecution("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/executions/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllExecutions() throws Exception {
        GraphExecution execution1 = GraphExecution.builder()
                .executionId("exec-1")
                .graphId("graph-1")
                .status(ExecutionStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .nodeExecutions(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        GraphExecution execution2 = GraphExecution.builder()
                .executionId("exec-2")
                .graphId("graph-2")
                .status(ExecutionStatus.COMPLETED)
                .startTime(LocalDateTime.now())
                .nodeExecutions(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        when(executionService.getAllExecutions()).thenReturn(Arrays.asList(execution1, execution2));

        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].executionId").value("exec-1"))
                .andExpect(jsonPath("$[1].executionId").value("exec-2"));
    }

    @Test
    void shouldCancelExecution() throws Exception {
        mockMvc.perform(delete("/api/executions/exec-123"))
                .andExpect(status().isNoContent());
    }
}
