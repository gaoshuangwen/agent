package dev.langgraph.ui.controller;

import dev.langgraph.core.*;
import dev.langgraph.ui.model.ExecutionStatus;
import dev.langgraph.ui.model.StartExecutionRequest;
import dev.langgraph.ui.service.GraphRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GraphRegistry graphRegistry;

    private Graph testGraph;

    @BeforeEach
    void setUp() {
        NodeId node1 = NodeId.generate();
        NodeId node2 = NodeId.generate();

        testGraph = GraphBuilder.newGraph()
                .id(GraphId.of("test-graph"))
                .name("Test Graph")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step", "1"))
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state.with("step", "2"))
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        graphRegistry.registerGraph("test-graph", testGraph);
    }

    @Test
    void testStartExecution() throws Exception {
        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "graphId": "test-graph",
                                    "initialState": {"input": "test"},
                                    "metadata": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").exists())
                .andExpect(jsonPath("$.executionId").isNotEmpty());
    }

    @Test
    void testStartExecutionWithInvalidGraph() throws Exception {
        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "graphId": "non-existent-graph",
                                    "initialState": {},
                                    "metadata": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testGetAllExecutions() throws Exception {
        mockMvc.perform(post("/api/executions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "graphId": "test-graph",
                            "initialState": {},
                            "metadata": {}
                        }
                        """));

        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetExecutionById() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "graphId": "test-graph",
                                    "initialState": {"data": "test"},
                                    "metadata": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String executionId = response.substring(response.indexOf(":\"") + 2, response.lastIndexOf("\""));

        await().until(() -> {
            MvcResult execResult = mockMvc.perform(get("/api/executions/" + executionId))
                    .andExpect(status().isOk())
                    .andReturn();
            String content = execResult.getResponse().getContentAsString();
            return content.contains("COMPLETED") || content.contains("FAILED");
        });

        mockMvc.perform(get("/api/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.graphId").value("test-graph"))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testGetNonExistentExecution() throws Exception {
        mockMvc.perform(get("/api/executions/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCancelExecution() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "graphId": "test-graph",
                                    "initialState": {},
                                    "metadata": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String executionId = response.substring(response.indexOf(":\"") + 2, response.lastIndexOf("\""));

        mockMvc.perform(post("/api/executions/" + executionId + "/cancel"))
                .andExpect(status().isOk());
    }
}
