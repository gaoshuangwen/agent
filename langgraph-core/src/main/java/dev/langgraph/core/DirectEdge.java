package dev.langgraph.core;

import java.util.Map;

public final class DirectEdge extends AbstractEdge {

    public DirectEdge(EdgeId id, NodeId source, NodeId target, Map<String, Object> metadata, int priority) {
        super(id, source, target, metadata, priority);
    }

    public DirectEdge(EdgeId id, NodeId source, NodeId target) {
        this(id, source, target, Map.of(), 0);
    }

    @Override
    public boolean canTraverse(State state, ExecutionContext context) {
        return true;
    }
}
