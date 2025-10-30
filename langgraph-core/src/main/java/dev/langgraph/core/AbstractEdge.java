package dev.langgraph.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractEdge implements Edge {
    
    private final EdgeId id;
    private final NodeId source;
    private final NodeId target;
    private final Map<String, Object> metadata;
    private final int priority;

    protected AbstractEdge(EdgeId id, NodeId source, NodeId target, Map<String, Object> metadata, int priority) {
        this.id = Objects.requireNonNull(id, "Edge ID cannot be null");
        this.source = Objects.requireNonNull(source, "Source node ID cannot be null");
        this.target = Objects.requireNonNull(target, "Target node ID cannot be null");
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        this.priority = priority;
    }

    @Override
    public EdgeId id() {
        return id;
    }

    @Override
    public NodeId source() {
        return source;
    }

    @Override
    public NodeId target() {
        return target;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEdge that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", " + source + " -> " + target + "}";
    }
}
