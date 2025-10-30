package dev.langgraph.core;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ExecutionContext {
    
    private final String executionId;
    private final GraphContext graphContext;
    private final Instant startTime;
    private final Map<String, Object> metadata;

    private ExecutionContext(String executionId, GraphContext graphContext, Instant startTime, Map<String, Object> metadata) {
        this.executionId = Objects.requireNonNull(executionId, "ExecutionId cannot be null");
        this.graphContext = Objects.requireNonNull(graphContext, "GraphContext cannot be null");
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.metadata = Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public static ExecutionContext create(GraphContext graphContext) {
        return new ExecutionContext(UUID.randomUUID().toString(), graphContext, Instant.now(), Map.of());
    }

    public static ExecutionContext create(GraphContext graphContext, Map<String, Object> metadata) {
        return new ExecutionContext(UUID.randomUUID().toString(), graphContext, Instant.now(), metadata);
    }

    public static ExecutionContext of(String executionId, GraphContext graphContext, Instant startTime, Map<String, Object> metadata) {
        return new ExecutionContext(executionId, graphContext, startTime, metadata);
    }

    public String executionId() {
        return executionId;
    }

    public GraphContext graphContext() {
        return graphContext;
    }

    public Instant startTime() {
        return startTime;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key) {
        return Optional.ofNullable((T) metadata.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }

    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    public ExecutionContext withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new ExecutionContext(executionId, graphContext, startTime, newMetadata);
    }

    public ExecutionContext withMetadata(Map<String, Object> additionalMetadata) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.putAll(additionalMetadata);
        return new ExecutionContext(executionId, graphContext, startTime, newMetadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext that = (ExecutionContext) o;
        return Objects.equals(executionId, that.executionId) &&
                Objects.equals(graphContext, that.graphContext) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, graphContext, startTime, metadata);
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "executionId='" + executionId + '\'' +
                ", graphContext=" + graphContext +
                ", startTime=" + startTime +
                ", metadata=" + metadata +
                '}';
    }
}
