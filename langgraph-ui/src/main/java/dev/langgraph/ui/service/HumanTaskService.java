package dev.langgraph.ui.service;

import dev.langgraph.core.human.HumanTask;
import dev.langgraph.core.human.HumanTaskManager;
import dev.langgraph.ui.model.HumanTaskDTO;
import dev.langgraph.ui.model.HumanTaskResponseRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HumanTaskService {

    private final HumanTaskManager humanTaskManager;

    public HumanTaskService(HumanTaskManager humanTaskManager) {
        this.humanTaskManager = humanTaskManager;
    }

    public List<HumanTaskDTO> getPendingTasks() {
        return humanTaskManager.getPendingTasks().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<HumanTaskDTO> getTasksForExecution(String executionId) {
        return humanTaskManager.getPendingTasksForExecution(executionId).stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<HumanTaskDTO> getTask(String taskId) {
        return humanTaskManager.getTask(taskId)
                .map(this::toDTO);
    }

    public void respondToTask(String taskId, HumanTaskResponseRequest request) {
        humanTaskManager.completeTask(taskId, request.response());
    }

    public void approveTask(String taskId) {
        humanTaskManager.completeTask(taskId, Map.of("approved", true));
    }

    public void rejectTask(String taskId) {
        humanTaskManager.completeTask(taskId, Map.of("approved", false, "reason", "Rejected by user"));
    }

    private HumanTaskDTO toDTO(HumanTask task) {
        return new HumanTaskDTO(
                task.taskId(),
                task.executionId(),
                task.nodeId(),
                task.type().name(),
                task.prompt(),
                task.context(),
                task.createdAt(),
                task.expiresAt(),
                task.status().name(),
                task.response(),
                task.completedAt()
        );
    }
}
