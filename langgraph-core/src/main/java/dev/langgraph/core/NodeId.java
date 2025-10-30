package dev.langgraph.core;

import java.util.Objects;
import java.util.UUID;

public record NodeId(String value) {
    
    public NodeId {
        Objects.requireNonNull(value, "NodeId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("NodeId value cannot be blank");
        }
    }

    public static NodeId of(String value) {
        return new NodeId(value);
    }

    public static NodeId generate() {
        return new NodeId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
