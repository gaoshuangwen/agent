package dev.langgraph.ui.model;

import java.util.Map;

public record EdgeDTO(
        String id,
        String source,
        String target,
        String type,
        Map<String, Object> metadata
) {
}
