package dev.langgraph.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public sealed interface GraphEvent permits 
    GraphEvent.NodeExecutionStarted,
    GraphEvent.NodeExecutionCompleted,
    GraphEvent.NodeExecutionFailed,
    GraphEvent.EdgeTraversed,
    GraphEvent.StateChanged,
    GraphEvent.GraphExecutionStarted,
    GraphEvent.GraphExecutionCompleted,
    GraphEvent.GraphExecutionFailed {

    Instant timestamp();
    String executionId();
    Map<String, Object> metadata();

    record NodeExecutionStarted(
            String executionId,
            String nodeId,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public NodeExecutionStarted {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(nodeId);
            Objects.requireNonNull(timestamp);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record NodeExecutionCompleted(
            String executionId,
            String nodeId,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public NodeExecutionCompleted {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(nodeId);
            Objects.requireNonNull(timestamp);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record NodeExecutionFailed(
            String executionId,
            String nodeId,
            Instant timestamp,
            Throwable error,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public NodeExecutionFailed {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(nodeId);
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(error);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record EdgeTraversed(
            String executionId,
            String edgeId,
            String fromNodeId,
            String toNodeId,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public EdgeTraversed {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(edgeId);
            Objects.requireNonNull(fromNodeId);
            Objects.requireNonNull(toNodeId);
            Objects.requireNonNull(timestamp);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record StateChanged(
            String executionId,
            String nodeId,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public StateChanged {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(timestamp);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record GraphExecutionStarted(
            String executionId,
            String graphId,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public GraphExecutionStarted {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(graphId);
            Objects.requireNonNull(timestamp);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record GraphExecutionCompleted(
            String executionId,
            String graphId,
            Instant timestamp,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public GraphExecutionCompleted {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(graphId);
            Objects.requireNonNull(timestamp);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }

    record GraphExecutionFailed(
            String executionId,
            String graphId,
            Instant timestamp,
            Throwable error,
            Map<String, Object> metadata
    ) implements GraphEvent {
        public GraphExecutionFailed {
            Objects.requireNonNull(executionId);
            Objects.requireNonNull(graphId);
            Objects.requireNonNull(timestamp);
            Objects.requireNonNull(error);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }
    }
}
