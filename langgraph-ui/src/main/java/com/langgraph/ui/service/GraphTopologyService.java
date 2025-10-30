package com.langgraph.ui.service;

import com.langgraph.ui.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphTopologyService {
    
    private final Map<String, GraphTopology> graphs = new HashMap<>();
    
    public GraphTopologyService() {
        initializeSampleGraphs();
    }
    
    public Optional<GraphTopology> getGraphTopology(String graphId) {
        return Optional.ofNullable(graphs.get(graphId));
    }
    
    public List<GraphTopology> getAllGraphs() {
        return new ArrayList<>(graphs.values());
    }
    
    private void initializeSampleGraphs() {
        GraphTopology sampleGraph = GraphTopology.builder()
                .graphId("sample-graph-1")
                .name("Sample LangGraph")
                .description("A sample graph demonstrating the visualization capabilities")
                .nodes(Arrays.asList(
                        GraphNode.builder()
                                .id("node1")
                                .name("Input Processing")
                                .type("processor")
                                .properties(Map.of("description", "Processes input data"))
                                .position(Position.builder().x(100).y(100).build())
                                .build(),
                        GraphNode.builder()
                                .id("node2")
                                .name("LLM Call")
                                .type("llm")
                                .properties(Map.of("model", "gpt-4", "temperature", 0.7))
                                .position(Position.builder().x(300).y(100).build())
                                .build(),
                        GraphNode.builder()
                                .id("node3")
                                .name("Output Formatting")
                                .type("formatter")
                                .properties(Map.of("format", "json"))
                                .position(Position.builder().x(500).y(100).build())
                                .build()
                ))
                .edges(Arrays.asList(
                        GraphEdge.builder()
                                .id("edge1")
                                .source("node1")
                                .target("node2")
                                .label("processed")
                                .build(),
                        GraphEdge.builder()
                                .id("edge2")
                                .source("node2")
                                .target("node3")
                                .label("response")
                                .build()
                ))
                .build();
        
        graphs.put("sample-graph-1", sampleGraph);
        
        GraphTopology complexGraph = GraphTopology.builder()
                .graphId("complex-graph-1")
                .name("Complex LangGraph")
                .description("A more complex graph with branching and loops")
                .nodes(Arrays.asList(
                        GraphNode.builder()
                                .id("start")
                                .name("Start")
                                .type("start")
                                .position(Position.builder().x(100).y(200).build())
                                .build(),
                        GraphNode.builder()
                                .id("decision")
                                .name("Decision Node")
                                .type("decision")
                                .position(Position.builder().x(300).y(200).build())
                                .build(),
                        GraphNode.builder()
                                .id("branch-a")
                                .name("Branch A")
                                .type("processor")
                                .position(Position.builder().x(500).y(100).build())
                                .build(),
                        GraphNode.builder()
                                .id("branch-b")
                                .name("Branch B")
                                .type("processor")
                                .position(Position.builder().x(500).y(300).build())
                                .build(),
                        GraphNode.builder()
                                .id("merge")
                                .name("Merge")
                                .type("merger")
                                .position(Position.builder().x(700).y(200).build())
                                .build()
                ))
                .edges(Arrays.asList(
                        GraphEdge.builder()
                                .id("edge-start-decision")
                                .source("start")
                                .target("decision")
                                .build(),
                        GraphEdge.builder()
                                .id("edge-decision-a")
                                .source("decision")
                                .target("branch-a")
                                .label("condition A")
                                .build(),
                        GraphEdge.builder()
                                .id("edge-decision-b")
                                .source("decision")
                                .target("branch-b")
                                .label("condition B")
                                .build(),
                        GraphEdge.builder()
                                .id("edge-a-merge")
                                .source("branch-a")
                                .target("merge")
                                .build(),
                        GraphEdge.builder()
                                .id("edge-b-merge")
                                .source("branch-b")
                                .target("merge")
                                .build()
                ))
                .build();
        
        graphs.put("complex-graph-1", complexGraph);
    }
}
