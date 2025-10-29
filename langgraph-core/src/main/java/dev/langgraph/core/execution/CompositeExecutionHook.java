package dev.langgraph.core.execution;

import dev.langgraph.core.ExecutionContext;
import dev.langgraph.core.NodeId;
import dev.langgraph.core.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CompositeExecutionHook implements ExecutionHook {
    
    private final List<ExecutionHook> hooks;

    public CompositeExecutionHook() {
        this.hooks = new CopyOnWriteArrayList<>();
    }

    public CompositeExecutionHook(List<ExecutionHook> hooks) {
        this.hooks = new CopyOnWriteArrayList<>(Objects.requireNonNull(hooks));
    }

    public void addHook(ExecutionHook hook) {
        Objects.requireNonNull(hook, "Hook cannot be null");
        hooks.add(hook);
    }

    public void removeHook(ExecutionHook hook) {
        hooks.remove(hook);
    }

    public List<ExecutionHook> getHooks() {
        return new ArrayList<>(hooks);
    }

    @Override
    public void onExecutionStart(ExecutionContext context, State initialState) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onExecutionStart(context, initialState)));
    }

    @Override
    public void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onExecutionComplete(context, result)));
    }

    @Override
    public void onExecutionFailed(ExecutionContext context, Throwable error) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onExecutionFailed(context, error)));
    }

    @Override
    public void onExecutionCancelled(ExecutionContext context) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onExecutionCancelled(context)));
    }

    @Override
    public void onNodeScheduled(ExecutionContext context, NodeId nodeId) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onNodeScheduled(context, nodeId)));
    }

    @Override
    public void onNodeStart(ExecutionContext context, NodeId nodeId, State inputState) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onNodeStart(context, nodeId, inputState)));
    }

    @Override
    public void onNodeComplete(ExecutionContext context, NodeId nodeId, State outputState) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onNodeComplete(context, nodeId, outputState)));
    }

    @Override
    public void onNodeFailed(ExecutionContext context, NodeId nodeId, Throwable error, int attemptNumber) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onNodeFailed(context, nodeId, error, attemptNumber)));
    }

    @Override
    public void onNodeRetry(ExecutionContext context, NodeId nodeId, int attemptNumber) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onNodeRetry(context, nodeId, attemptNumber)));
    }

    @Override
    public void onStateSnapshot(ExecutionContext context, StateSnapshot snapshot) {
        hooks.forEach(hook -> safeInvoke(() -> hook.onStateSnapshot(context, snapshot)));
    }

    private void safeInvoke(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            System.err.println("Error in execution hook: " + e.getMessage());
        }
    }
}
