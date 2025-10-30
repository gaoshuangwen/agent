package dev.langgraph.ui.service;

import dev.langgraph.core.ExecutionContext;
import dev.langgraph.core.NodeId;
import dev.langgraph.core.State;
import dev.langgraph.core.execution.ExecutionHook;
import dev.langgraph.core.execution.ExecutionResult;
import dev.langgraph.core.execution.StateSnapshot;
import dev.langgraph.ui.model.NodeStatus;

public class TrackingExecutionHook implements ExecutionHook {

    private final ExecutionTracker tracker;
    private final WebSocketNotificationService notificationService;

    public TrackingExecutionHook(ExecutionTracker tracker, WebSocketNotificationService notificationService) {
        this.tracker = tracker;
        this.notificationService = notificationService;
    }

    @Override
    public void onExecutionStart(ExecutionContext context, State initialState) {
        tracker.updateState(context.executionId(), initialState);
    }

    @Override
    public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
        tracker.updateState(context.executionId(), result.finalState());
    }

    @Override
    public void onExecutionFailed(ExecutionContext context, Throwable error) {
        tracker.setError(context.executionId(), error.getMessage());
    }

    @Override
    public void onExecutionCancelled(ExecutionContext context) {
    }

    @Override
    public void onNodeScheduled(ExecutionContext context, NodeId nodeId) {
        tracker.updateNodeStatus(context.executionId(), nodeId.value(), NodeStatus.PENDING);
    }

    @Override
    public void onNodeStart(ExecutionContext context, NodeId nodeId, State state) {
        tracker.updateNodeStatus(context.executionId(), nodeId.value(), NodeStatus.RUNNING);
    }

    @Override
    public void onNodeComplete(ExecutionContext context, NodeId nodeId, State state) {
        tracker.updateNodeStatus(context.executionId(), nodeId.value(), NodeStatus.COMPLETED);
        tracker.updateState(context.executionId(), state);
    }

    @Override
    public void onNodeFailed(ExecutionContext context, NodeId nodeId, Throwable error, int attempt) {
        tracker.updateNodeStatus(context.executionId(), nodeId.value(), NodeStatus.FAILED);
    }

    @Override
    public void onNodeRetry(ExecutionContext context, NodeId nodeId, int attempt) {
    }

    @Override
    public void onStateSnapshot(ExecutionContext context, StateSnapshot snapshot) {
    }
}
