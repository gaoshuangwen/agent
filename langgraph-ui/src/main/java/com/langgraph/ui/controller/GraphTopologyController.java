package com.langgraph.ui.controller;

import com.langgraph.ui.model.GraphTopology;
import com.langgraph.ui.service.GraphTopologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graphs")
@CrossOrigin(origins = "*")
public class GraphTopologyController {
    
    private final GraphTopologyService topologyService;
    
    public GraphTopologyController(GraphTopologyService topologyService) {
        this.topologyService = topologyService;
    }
    
    @GetMapping("/{graphId}")
    public ResponseEntity<GraphTopology> getGraphTopology(@PathVariable String graphId) {
        return topologyService.getGraphTopology(graphId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<GraphTopology>> getAllGraphs() {
        List<GraphTopology> graphs = topologyService.getAllGraphs();
        return ResponseEntity.ok(graphs);
    }
}
