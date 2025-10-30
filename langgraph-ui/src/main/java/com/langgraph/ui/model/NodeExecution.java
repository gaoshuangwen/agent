package com.langgraph.ui.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecution {
    private String nodeId;
    private String nodeName;
    private NodeStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private String error;
    private Long duration;
}
