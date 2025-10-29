package dev.langgraph.core;

import dev.langgraph.core.event.EventPublisher;
import dev.langgraph.core.event.GraphEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractNode implements Node {
    
    private final NodeId id;
    private final String name;
    private final Map<String, Object> metadata;

    protected AbstractNode(NodeId id, String name, Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    @Override
    public NodeId id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public final State execute(State inputState, ExecutionContext context, EventPublisher eventPublisher) throws Exception {
        Objects.requireNonNull(inputState, "Input state cannot be null");
        Objects.requireNonNull(context, "Execution context cannot be null");
        Objects.requireNonNull(eventPublisher, "Event publisher cannot be null");

        eventPublisher.publish(new GraphEvent.NodeExecutionStarted(
                context.executionId(),
                id.value(),
                Instant.now(),
                Map.of()
        ));

        try {
            onBeforeExecute(inputState, context);
            
            State outputState = doExecute(inputState, context);
            
            onAfterExecute(inputState, outputState, context);

            eventPublisher.publish(new GraphEvent.NodeExecutionCompleted(
                    context.executionId(),
                    id.value(),
                    Instant.now(),
                    Map.of()
            ));

            return outputState;
        } catch (Exception e) {
            onError(inputState, context, e);
            
            eventPublisher.publish(new GraphEvent.NodeExecutionFailed(
                    context.executionId(),
                    id.value(),
                    Instant.now(),
                    e,
                    Map.of()
            ));
            
            throw e;
        }
    }

    protected abstract State doExecute(State inputState, ExecutionContext context) throws Exception;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractNode that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name='" + name + "'}";
    }
}
