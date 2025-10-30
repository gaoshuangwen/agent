package dev.langgraph.ui.model;

import java.time.Instant;
import java.util.Map;

public record ExecutionEventDTO(
        String eventType,
        String executionId,
        String nodeId,
        Instant timestamp,
        Map<String, Object> metadata
) {
}
