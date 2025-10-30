package com.langgraph.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphTopology {
    private String graphId;
    private String name;
    private String description;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
}
