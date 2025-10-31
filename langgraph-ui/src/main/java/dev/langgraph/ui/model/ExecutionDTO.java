package dev.langgraph.ui.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExecutionDTO(
        String executionId,
        String graphId,
        ExecutionStatus status,
        Instant startTime,
        Instant endTime,
        String currentNode,
        Map<String, Object> state,
        List<ExecutionEventDTO> events,
        Map<String, NodeStatus> nodeStatuses,
        String error
) {
}
