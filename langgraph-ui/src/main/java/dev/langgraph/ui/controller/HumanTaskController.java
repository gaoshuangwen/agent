package dev.langgraph.ui.controller;

import dev.langgraph.ui.model.HumanTaskDTO;
import dev.langgraph.ui.model.HumanTaskResponseRequest;
import dev.langgraph.ui.service.HumanTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class HumanTaskController {

    private final HumanTaskService humanTaskService;

    public HumanTaskController(HumanTaskService humanTaskService) {
        this.humanTaskService = humanTaskService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HumanTaskDTO>> getPendingTasks() {
        return ResponseEntity.ok(humanTaskService.getPendingTasks());
    }

    @GetMapping("/execution/{executionId}")
    public ResponseEntity<List<HumanTaskDTO>> getTasksForExecution(@PathVariable("executionId") String executionId) {
        return ResponseEntity.ok(humanTaskService.getTasksForExecution(executionId));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<HumanTaskDTO> getTask(@PathVariable("taskId") String taskId) {
        return humanTaskService.getTask(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{taskId}/respond")
    public ResponseEntity<Void> respondToTask(
            @PathVariable("taskId") String taskId,
            @RequestBody HumanTaskResponseRequest request) {
        humanTaskService.respondToTask(taskId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/approve")
    public ResponseEntity<Void> approveTask(@PathVariable("taskId") String taskId) {
        humanTaskService.approveTask(taskId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{taskId}/reject")
    public ResponseEntity<Void> rejectTask(@PathVariable("taskId") String taskId) {
        humanTaskService.rejectTask(taskId);
        return ResponseEntity.ok().build();
    }
}
