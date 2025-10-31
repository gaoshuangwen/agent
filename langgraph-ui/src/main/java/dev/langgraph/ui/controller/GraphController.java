package dev.langgraph.ui.controller;

import dev.langgraph.ui.model.GraphTopologyDTO;
import dev.langgraph.ui.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graphs")
@CrossOrigin(origins = "*")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public ResponseEntity<List<GraphTopologyDTO>> getAllGraphs() {
        return ResponseEntity.ok(graphService.getAllGraphs());
    }

    @GetMapping("/{graphId}")
    public ResponseEntity<GraphTopologyDTO> getGraphTopology(@PathVariable("graphId")
                                                                 String graphId) {
        return graphService.getGraphTopology(graphId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
