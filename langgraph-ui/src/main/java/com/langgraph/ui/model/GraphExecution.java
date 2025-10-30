package com.langgraph.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphExecution {
    private String executionId;
    private String graphId;
    private ExecutionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private String error;
    
    @Builder.Default
    private List<NodeExecution> nodeExecutions = new ArrayList<>();
    
    @Builder.Default
    private List<ExecutionEvent> events = new ArrayList<>();
}
