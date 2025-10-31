package dev.langgraph.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GraphContext {
    
    private final GraphId graphId;
    private final Map<String, Object> attributes;

    private GraphContext(GraphId graphId, Map<String, Object> attributes) {
        this.graphId = Objects.requireNonNull(graphId, "GraphId cannot be null");
        this.attributes = Map.copyOf(attributes);
    }

    public static GraphContext of(GraphId graphId) {
        return new GraphContext(graphId, Map.of());
    }

    public static GraphContext of(GraphId graphId, Map<String, Object> attributes) {
        return new GraphContext(graphId, attributes);
    }

    public GraphId graphId() {
        return graphId;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        return (T) attributes.getOrDefault(key, defaultValue);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public GraphContext withAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put(key, value);
        return new GraphContext(graphId, newAttributes);
    }

    public GraphContext withAttributes(Map<String, Object> additionalAttributes) {
        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.putAll(additionalAttributes);
        return new GraphContext(graphId, newAttributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphContext that = (GraphContext) o;
        return Objects.equals(graphId, that.graphId) && Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphId, attributes);
    }

    @Override
    public String toString() {
        return "GraphContext{graphId=" + graphId + ", attributes=" + attributes + '}';
    }
}
