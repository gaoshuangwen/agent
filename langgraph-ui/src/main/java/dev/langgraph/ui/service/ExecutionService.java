package dev.langgraph.ui.service;

import dev.langgraph.core.*;
import dev.langgraph.core.event.EventPublisher;
import dev.langgraph.core.execution.ExecutionConfig;
import dev.langgraph.core.execution.StateGraphExecutor;
import dev.langgraph.ui.model.ExecutionDTO;
import dev.langgraph.ui.model.ExecutionStatus;
import dev.langgraph.ui.model.StartExecutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionTracker executionTracker;
    private final GraphRegistry graphRegistry;
    private final WebSocketNotificationService notificationService;

    public ExecutionService(ExecutionTracker executionTracker,
                           GraphRegistry graphRegistry,
                           WebSocketNotificationService notificationService) {
        this.executionTracker = executionTracker;
        this.graphRegistry = graphRegistry;
        this.notificationService = notificationService;
    }

    public String startExecution(StartExecutionRequest request) {
        Graph graph = graphRegistry.getGraph(request.graphId())
                .orElseThrow(() -> new IllegalArgumentException("Graph not found: " + request.graphId()));

        State initialState = new State(request.initialState() != null ? request.initialState() : Map.of());
        GraphContext graphContext = GraphContext.of(GraphId.of(request.graphId()));

        EventPublisher eventPublisher = event -> {
            notificationService.publishEvent(event);
            executionTracker.recordEvent(event.executionId(), event);
        };

        ExecutionConfig config = ExecutionConfig.builder()
                .maxConcurrency(10)
                .enableSnapshots(false)
                .build();

        StateGraphExecutor executor = new StateGraphExecutor(graph, config, eventPublisher, new TrackingExecutionHook(executionTracker, notificationService));

        ExecutionContext context = ExecutionContext.create(graphContext, request.metadata() != null ? request.metadata() : Map.of());
        String executionId = context.executionId();

        executionTracker.startExecution(executionId, request.graphId(), initialState);
        executionTracker.updateExecutionStatus(executionId, ExecutionStatus.RUNNING);

        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting execution {} for graph {}", executionId, request.graphId());
                executor.execute(initialState, graphContext);
                executionTracker.updateExecutionStatus(executionId, ExecutionStatus.COMPLETED);
                logger.info("Execution {} completed successfully", executionId);
            } catch (Exception e) {
                logger.error("Execution {} failed", executionId, e);
                executionTracker.updateExecutionStatus(executionId, ExecutionStatus.FAILED);
                executionTracker.setError(executionId, e.getMessage());
            }
        });

        return executionId;
    }

    public Optional<ExecutionDTO> getExecution(String executionId) {
        return executionTracker.getExecution(executionId)
                .map(this::toDTO);
    }

    public List<ExecutionDTO> getAllExecutions() {
        return executionTracker.getAllExecutions().stream()
                .map(this::toDTO)
                .toList();
    }

    public void cancelExecution(String executionId) {
        executionTracker.updateExecutionStatus(executionId, ExecutionStatus.CANCELLED);
    }

    private ExecutionDTO toDTO(ExecutionTracker.ExecutionInfo info) {
        return new ExecutionDTO(
                info.getExecutionId(),
                info.getGraphId(),
                info.getStatus(),
                info.getStartTime(),
                info.getEndTime(),
                info.getCurrentNode(),
                info.getCurrentState() != null ? info.getCurrentState().data() : Map.of(),
                info.getEvents(),
                info.getNodeStatuses(),
                info.getError()
        );
    }
}
