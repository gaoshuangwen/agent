package dev.langgraph.core.execution;

import dev.langgraph.core.ExecutionContext;
import dev.langgraph.core.NodeId;
import dev.langgraph.core.State;

public interface ExecutionHook {
    
    default void onExecutionStart(ExecutionContext context, State initialState) {
    }
    
    default void onExecutionComplete(ExecutionContext context, ExecutionResult result) {
    }
    
    default void onExecutionFailed(ExecutionContext context, Throwable error) {
    }
    
    default void onExecutionCancelled(ExecutionContext context) {
    }
    
    default void onNodeScheduled(ExecutionContext context, NodeId nodeId) {
    }
    
    default void onNodeStart(ExecutionContext context, NodeId nodeId, State inputState) {
    }
    
    default void onNodeComplete(ExecutionContext context, NodeId nodeId, State outputState) {
    }
    
    default void onNodeFailed(ExecutionContext context, NodeId nodeId, Throwable error, int attemptNumber) {
    }
    
    default void onNodeRetry(ExecutionContext context, NodeId nodeId, int attemptNumber) {
    }
    
    default void onStateSnapshot(ExecutionContext context, StateSnapshot snapshot) {
    }
    
    static ExecutionHook noOp() {
        return new ExecutionHook() {};
    }
}
