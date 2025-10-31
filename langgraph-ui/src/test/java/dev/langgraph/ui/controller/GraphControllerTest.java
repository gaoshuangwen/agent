package dev.langgraph.ui.controller;

import dev.langgraph.core.*;
import dev.langgraph.ui.service.GraphRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GraphRegistry graphRegistry;

    @BeforeEach
    void setUp() {
        NodeId node1 = NodeId.generate();
        NodeId node2 = NodeId.generate();

        Graph graph = GraphBuilder.newGraph()
                .id(GraphId.of("test-graph-topology"))
                .name("Test Graph Topology")
                .addFunctionalNode(node1, "StartNode", (state, ctx) -> state)
                .addFunctionalNode(node2, "EndNode", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        graphRegistry.registerGraph("test-graph-topology", graph);
    }

    @Test
    void testGetAllGraphs() throws Exception {
        mockMvc.perform(get("/api/graphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].graphId").value("test-graph-topology"))
                .andExpect(jsonPath("$[0].name").value("Test Graph Topology"))
                .andExpect(jsonPath("$[0].nodes").isArray())
                .andExpect(jsonPath("$[0].edges").isArray())
                .andExpect(jsonPath("$[0].entryPoint").exists());
    }

    @Test
    void testGetGraphTopology() throws Exception {
        mockMvc.perform(get("/api/graphs/test-graph-topology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graphId").value("test-graph-topology"))
                .andExpect(jsonPath("$.name").value("Test Graph Topology"))
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.edges", hasSize(1)))
                .andExpect(jsonPath("$.nodes[0].name").exists())
                .andExpect(jsonPath("$.edges[0].source").exists())
                .andExpect(jsonPath("$.edges[0].target").exists());
    }

    @Test
    void testGetNonExistentGraph() throws Exception {
        mockMvc.perform(get("/api/graphs/non-existent"))
                .andExpect(status().isNotFound());
    }
}
