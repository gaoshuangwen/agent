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
public class ExecutionEvent {
    private String eventId;
    private String executionId;
    private EventType type;
    private String nodeId;
    private LocalDateTime timestamp;
    private String message;
    private Map<String, Object> metadata;
}
