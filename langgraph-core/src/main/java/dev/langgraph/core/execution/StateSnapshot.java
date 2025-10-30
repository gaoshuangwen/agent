package dev.langgraph.core.execution;

import dev.langgraph.core.NodeId;
import dev.langgraph.core.State;

import java.time.Instant;
import java.util.Objects;

public record StateSnapshot(
        String snapshotId,
        String executionId,
        NodeId nodeId,
        State state,
        Instant timestamp
) {
    public StateSnapshot {
        Objects.requireNonNull(snapshotId, "Snapshot ID cannot be null");
        Objects.requireNonNull(executionId, "Execution ID cannot be null");
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    public static StateSnapshot create(String executionId, NodeId nodeId, State state) {
        return new StateSnapshot(
                java.util.UUID.randomUUID().toString(),
                executionId,
                nodeId,
                state,
                Instant.now()
        );
    }
}
