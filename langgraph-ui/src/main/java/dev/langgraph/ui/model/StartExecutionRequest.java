package dev.langgraph.ui.model;

import java.util.Map;

public record StartExecutionRequest(
        String graphId,
        Map<String, Object> initialState,
        Map<String, Object> metadata
) {
}
