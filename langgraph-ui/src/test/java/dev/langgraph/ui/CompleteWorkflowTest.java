package dev.langgraph.ui;

import dev.langgraph.core.*;
import dev.langgraph.ui.service.GraphRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CompleteWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GraphRegistry graphRegistry;

    private String testGraphId = "test-workflow-graph";

    @BeforeEach
    void setUp() {
        NodeId start = NodeId.of("start");
        NodeId process = NodeId.of("process");
        NodeId end = NodeId.of("end");

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of(testGraphId))
                .name("Complete Workflow Test Graph")
                .addFunctionalNode(start, "Start Node", (state, ctx) -> 
                    state.with("step", "started").with("value", 1))
                .addFunctionalNode(process, "Process Node", (state, ctx) -> {
                    Integer value = state.get("value", 0);
                    return state.with("step", "processed").with("value", value + 10);
                })
                .addFunctionalNode(end, "End Node", (state, ctx) -> 
                    state.with("step", "completed").with("result", "success"))
                .addDirectEdge(start, process)
                .addDirectEdge(process, end)
                .entryPoint(start)
                .build();

        graphRegistry.registerGraph(testGraphId, graph);
    }

    @Test
    void testCompleteWorkflow() throws Exception {
        // Step 1: Get available graphs
        mockMvc.perform(get("/api/graphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.graphId == '" + testGraphId + "')].name")
                        .value(hasItem("Complete Workflow Test Graph")));

        // Step 2: Get specific graph topology
        mockMvc.perform(get("/api/graphs/" + testGraphId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graphId").value(testGraphId))
                .andExpect(jsonPath("$.nodes", hasSize(3)))
                .andExpect(jsonPath("$.edges", hasSize(2)))
                .andExpect(jsonPath("$.entryPoint").value("start"));

        // Step 3: Start execution
        MvcResult startResult = mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "graphId": "%s",
                                    "initialState": {"input": "test"},
                                    "metadata": {"test": true}
                                }
                                """.formatted(testGraphId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").exists())
                .andReturn();

        String response = startResult.getResponse().getContentAsString();
        String executionId = response.substring(response.indexOf(":\"") + 2, response.lastIndexOf("\""));

        // Step 4: Wait for execution to complete
        await().untilAsserted(() -> {
            MvcResult result = mockMvc.perform(get("/api/executions/" + executionId))
                    .andExpect(status().isOk())
                    .andReturn();
            String content = result.getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertTrue(
                    content.contains("COMPLETED") || content.contains("FAILED"),
                    "Execution should complete"
            );
        });

        // Step 5: Verify execution details
        mockMvc.perform(get("/api/executions/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.graphId").value(testGraphId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.state.step").value("completed"))
                .andExpect(jsonPath("$.state.value").value(11))
                .andExpect(jsonPath("$.state.result").value("success"))
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.nodeStatuses.start").value("COMPLETED"))
                .andExpect(jsonPath("$.nodeStatuses.process").value("COMPLETED"))
                .andExpect(jsonPath("$.nodeStatuses.end").value("COMPLETED"));

        // Step 6: Verify UI static resources are accessible
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LangGraph UI")))
                .andExpect(content().string(containsString("Graphs")))
                .andExpect(content().string(containsString("Executions")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fetchGraphs")))
                .andExpect(content().string(containsString("startExecution")));
    }
}
