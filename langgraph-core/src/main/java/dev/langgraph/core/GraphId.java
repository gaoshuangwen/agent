package dev.langgraph.core;

import java.util.Objects;
import java.util.UUID;

public record GraphId(String value) {
    
    public GraphId {
        Objects.requireNonNull(value, "GraphId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("GraphId value cannot be blank");
        }
    }

    public static GraphId of(String value) {
        return new GraphId(value);
    }

    public static GraphId generate() {
        return new GraphId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
