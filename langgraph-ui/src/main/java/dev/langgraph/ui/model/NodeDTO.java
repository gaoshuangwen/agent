package dev.langgraph.ui.model;

import java.util.Map;

public record NodeDTO(
        String id,
        String name,
        Map<String, Object> metadata,
        NodeStatus status
) {
    public NodeDTO(String id, String name, Map<String, Object> metadata) {
        this(id, name, metadata, NodeStatus.PENDING);
    }
}
