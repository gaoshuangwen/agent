package dev.langgraph.core;

import java.util.Objects;
import java.util.UUID;

public record EdgeId(String value) {
    
    public EdgeId {
        Objects.requireNonNull(value, "EdgeId value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("EdgeId value cannot be blank");
        }
    }

    public static EdgeId of(String value) {
        return new EdgeId(value);
    }

    public static EdgeId generate() {
        return new EdgeId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
