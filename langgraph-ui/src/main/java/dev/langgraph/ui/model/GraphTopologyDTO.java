package dev.langgraph.ui.model;

import java.util.List;
import java.util.Map;

public record GraphTopologyDTO(
        String graphId,
        String name,
        List<NodeDTO> nodes,
        List<EdgeDTO> edges,
        String entryPoint,
        Map<String, Object> metadata
) {
}
