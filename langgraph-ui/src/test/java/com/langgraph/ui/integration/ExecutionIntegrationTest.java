package com.langgraph.ui.integration;

import com.langgraph.ui.dto.StartExecutionRequest;
import com.langgraph.ui.model.ExecutionStatus;
import com.langgraph.ui.model.GraphExecution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExecutionIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldStartAndMonitorExecution() throws InterruptedException {
        StartExecutionRequest request = StartExecutionRequest.builder()
                .graphId("sample-graph-1")
                .input(Map.of("message", "test"))
                .build();

        ResponseEntity<GraphExecution> response = restTemplate.postForEntity(
                "/api/executions",
                request,
                GraphExecution.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("sample-graph-1", response.getBody().getGraphId());
        assertNotNull(response.getBody().getExecutionId());

        String executionId = response.getBody().getExecutionId();

        Thread.sleep(1000);

        ResponseEntity<GraphExecution> getResponse = restTemplate.getForEntity(
                "/api/executions/" + executionId,
                GraphExecution.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(executionId, getResponse.getBody().getExecutionId());
    }

    @Test
    void shouldGetAllExecutions() {
        ResponseEntity<GraphExecution[]> response = restTemplate.getForEntity(
                "/api/executions",
                GraphExecution[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldReturnNotFoundForNonExistentExecution() {
        ResponseEntity<GraphExecution> response = restTemplate.getForEntity(
                "/api/executions/non-existent-id",
                GraphExecution.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
