package com.langgraph.ui.service;

import com.langgraph.ui.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GraphExecutionService {
    
    private final Map<String, GraphExecution> executions = new ConcurrentHashMap<>();
    private final TelemetryService telemetryService;
    
    public GraphExecutionService(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }
    
    public GraphExecution startExecution(String graphId, Map<String, Object> input) {
        String executionId = UUID.randomUUID().toString();
        
        GraphExecution execution = GraphExecution.builder()
                .executionId(executionId)
                .graphId(graphId)
                .status(ExecutionStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .input(input)
                .nodeExecutions(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
        
        executions.put(executionId, execution);
        
        ExecutionEvent event = ExecutionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .executionId(executionId)
                .type(EventType.EXECUTION_STARTED)
                .timestamp(LocalDateTime.now())
                .message("Execution started for graph: " + graphId)
                .build();
        
        execution.getEvents().add(event);
        telemetryService.publishEvent(event);
        
        log.info("Started execution {} for graph {}", executionId, graphId);
        
        simulateExecution(execution);
        
        return execution;
    }
    
    public Optional<GraphExecution> getExecution(String executionId) {
        return Optional.ofNullable(executions.get(executionId));
    }
    
    public List<GraphExecution> getAllExecutions() {
        return new ArrayList<>(executions.values());
    }
    
    public void cancelExecution(String executionId) {
        GraphExecution execution = executions.get(executionId);
        if (execution != null && execution.getStatus() == ExecutionStatus.RUNNING) {
            execution.setStatus(ExecutionStatus.CANCELLED);
            execution.setEndTime(LocalDateTime.now());
            
            ExecutionEvent event = ExecutionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .executionId(executionId)
                    .type(EventType.EXECUTION_FAILED)
                    .timestamp(LocalDateTime.now())
                    .message("Execution cancelled")
                    .build();
            
            execution.getEvents().add(event);
            telemetryService.publishEvent(event);
            
            log.info("Cancelled execution {}", executionId);
        }
    }
    
    private void simulateExecution(GraphExecution execution) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                
                List<String> nodeIds = Arrays.asList("node1", "node2", "node3");
                List<String> nodeNames = Arrays.asList("Input Processing", "LLM Call", "Output Formatting");
                
                for (int i = 0; i < nodeIds.size(); i++) {
                    String nodeId = nodeIds.get(i);
                    String nodeName = nodeNames.get(i);
                    
                    NodeExecution nodeExecution = NodeExecution.builder()
                            .nodeId(nodeId)
                            .nodeName(nodeName)
                            .status(NodeStatus.RUNNING)
                            .startTime(LocalDateTime.now())
                            .input(Map.of("data", "sample input"))
                            .build();
                    
                    execution.getNodeExecutions().add(nodeExecution);
                    
                    ExecutionEvent startEvent = ExecutionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .executionId(execution.getExecutionId())
                            .type(EventType.NODE_STARTED)
                            .nodeId(nodeId)
                            .timestamp(LocalDateTime.now())
                            .message("Node started: " + nodeName)
                            .build();
                    
                    execution.getEvents().add(startEvent);
                    telemetryService.publishEvent(startEvent);
                    
                    Thread.sleep(2000);
                    
                    nodeExecution.setStatus(NodeStatus.COMPLETED);
                    nodeExecution.setEndTime(LocalDateTime.now());
                    nodeExecution.setOutput(Map.of("result", "sample output"));
                    nodeExecution.setDuration(2000L);
                    
                    ExecutionEvent completeEvent = ExecutionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .executionId(execution.getExecutionId())
                            .type(EventType.NODE_COMPLETED)
                            .nodeId(nodeId)
                            .timestamp(LocalDateTime.now())
                            .message("Node completed: " + nodeName)
                            .build();
                    
                    execution.getEvents().add(completeEvent);
                    telemetryService.publishEvent(completeEvent);
                }
                
                execution.setStatus(ExecutionStatus.COMPLETED);
                execution.setEndTime(LocalDateTime.now());
                execution.setOutput(Map.of("result", "Final output"));
                
                ExecutionEvent completeEvent = ExecutionEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .executionId(execution.getExecutionId())
                        .type(EventType.EXECUTION_COMPLETED)
                        .timestamp(LocalDateTime.now())
                        .message("Execution completed successfully")
                        .build();
                
                execution.getEvents().add(completeEvent);
                telemetryService.publishEvent(completeEvent);
                
                log.info("Completed execution {}", execution.getExecutionId());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setEndTime(LocalDateTime.now());
                execution.setError(e.getMessage());
                
                log.error("Execution {} failed", execution.getExecutionId(), e);
            }
        }).start();
    }
}
