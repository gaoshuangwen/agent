package dev.langgraph.ui.model;

import java.time.Instant;
import java.util.Map;

public record HumanTaskDTO(
        String taskId,
        String executionId,
        String nodeId,
        String type,
        String prompt,
        Map<String, Object> context,
        Instant createdAt,
        Instant expiresAt,
        String status,
        Map<String, Object> response,
        Instant completedAt
) {
}
