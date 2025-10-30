package com.langgraph.ui.controller;

import com.langgraph.ui.model.ExecutionEvent;
import com.langgraph.ui.service.TelemetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "*")
public class TelemetryController {
    
    private final TelemetryService telemetryService;
    
    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }
    
    @GetMapping("/executions/{executionId}/events")
    public ResponseEntity<List<ExecutionEvent>> getExecutionEvents(@PathVariable String executionId) {
        List<ExecutionEvent> events = telemetryService.getExecutionEvents(executionId);
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/events")
    public ResponseEntity<List<ExecutionEvent>> getAllEvents() {
        List<ExecutionEvent> events = telemetryService.getAllEvents();
        return ResponseEntity.ok(events);
    }
}
