package dev.langgraph.ui.controller;

import dev.langgraph.ui.model.ExecutionDTO;
import dev.langgraph.ui.model.StartExecutionRequest;
import dev.langgraph.ui.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/executions")
@CrossOrigin(origins = "*")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> startExecution(@RequestBody StartExecutionRequest request) {
        try {
            String executionId = executionService.startExecution(request);
            return ResponseEntity.ok(Map.of("executionId", executionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ExecutionDTO>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionDTO> getExecution(@PathVariable("executionId") String executionId) {
        return executionService.getExecution(executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{executionId}/cancel")
    public ResponseEntity<Void> cancelExecution(@PathVariable("executionId") String executionId) {
        executionService.cancelExecution(executionId);
        return ResponseEntity.ok().build();
    }
}
