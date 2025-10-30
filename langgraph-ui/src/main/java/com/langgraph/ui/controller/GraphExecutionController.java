package com.langgraph.ui.controller;

import com.langgraph.ui.dto.StartExecutionRequest;
import com.langgraph.ui.model.GraphExecution;
import com.langgraph.ui.service.GraphExecutionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/executions")
@CrossOrigin(origins = "*")
public class GraphExecutionController {
    
    private final GraphExecutionService executionService;
    
    public GraphExecutionController(GraphExecutionService executionService) {
        this.executionService = executionService;
    }
    
    @PostMapping
    public ResponseEntity<GraphExecution> startExecution(@Valid @RequestBody StartExecutionRequest request) {
        log.info("Starting execution for graph: {}", request.getGraphId());
        GraphExecution execution = executionService.startExecution(request.getGraphId(), request.getInput());
        return ResponseEntity.status(HttpStatus.CREATED).body(execution);
    }
    
    @GetMapping("/{executionId}")
    public ResponseEntity<GraphExecution> getExecution(@PathVariable String executionId) {
        return executionService.getExecution(executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<GraphExecution>> getAllExecutions() {
        List<GraphExecution> executions = executionService.getAllExecutions();
        return ResponseEntity.ok(executions);
    }
    
    @DeleteMapping("/{executionId}")
    public ResponseEntity<Void> cancelExecution(@PathVariable String executionId) {
        executionService.cancelExecution(executionId);
        return ResponseEntity.noContent().build();
    }
}
