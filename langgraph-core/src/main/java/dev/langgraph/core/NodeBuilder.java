package dev.langgraph.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public final class NodeBuilder {
    
    private NodeId id;
    private String name;
    private final Map<String, Object> metadata = new HashMap<>();
    private BiFunction<State, ExecutionContext, State> function;

    private NodeBuilder() {
    }

    public static NodeBuilder newNode() {
        return new NodeBuilder();
    }

    public static NodeBuilder newNode(String name) {
        return new NodeBuilder().name(name);
    }

    public NodeBuilder id(NodeId id) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        return this;
    }

    public NodeBuilder name(String name) {
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        return this;
    }

    public NodeBuilder metadata(String key, Object value) {
        Objects.requireNonNull(key, "Metadata key cannot be null");
        this.metadata.put(key, value);
        return this;
    }

    public NodeBuilder metadata(Map<String, Object> metadata) {
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        this.metadata.putAll(metadata);
        return this;
    }

    public NodeBuilder function(BiFunction<State, ExecutionContext, State> function) {
        this.function = Objects.requireNonNull(function, "Function cannot be null");
        return this;
    }

    public Node build() {
        if (id == null) {
            id = NodeId.generate();
        }
        if (name == null) {
            name = "Node-" + id.value();
        }
        if (function == null) {
            throw new IllegalStateException("Node function must be set");
        }

        return new FunctionalNode(id, name, metadata, function);
    }
}
