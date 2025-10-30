package com.langgraph.ui.controller;

import com.langgraph.ui.model.*;
import com.langgraph.ui.service.GraphTopologyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraphTopologyController.class)
class GraphTopologyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GraphTopologyService topologyService;

    @Test
    void shouldGetGraphTopology() throws Exception {
        GraphTopology topology = GraphTopology.builder()
                .graphId("graph-1")
                .name("Test Graph")
                .description("Test description")
                .nodes(Arrays.asList(
                        GraphNode.builder()
                                .id("node-1")
                                .name("Node 1")
                                .type("processor")
                                .build()
                ))
                .edges(Arrays.asList(
                        GraphEdge.builder()
                                .id("edge-1")
                                .source("node-1")
                                .target("node-2")
                                .build()
                ))
                .build();

        when(topologyService.getGraphTopology("graph-1")).thenReturn(Optional.of(topology));

        mockMvc.perform(get("/api/graphs/graph-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graphId").value("graph-1"))
                .andExpect(jsonPath("$.name").value("Test Graph"))
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.edges.length()").value(1));
    }

    @Test
    void shouldReturnNotFoundForNonExistentGraph() throws Exception {
        when(topologyService.getGraphTopology("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/graphs/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllGraphs() throws Exception {
        GraphTopology graph1 = GraphTopology.builder()
                .graphId("graph-1")
                .name("Graph 1")
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .build();

        GraphTopology graph2 = GraphTopology.builder()
                .graphId("graph-2")
                .name("Graph 2")
                .nodes(new ArrayList<>())
                .edges(new ArrayList<>())
                .build();

        when(topologyService.getAllGraphs()).thenReturn(Arrays.asList(graph1, graph2));

        mockMvc.perform(get("/api/graphs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].graphId").value("graph-1"))
                .andExpect(jsonPath("$[1].graphId").value("graph-2"));
    }
}
