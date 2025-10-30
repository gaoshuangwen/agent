package dev.langgraph.core;

import dev.langgraph.core.event.EventPublisher;

import java.util.Map;

public interface Node {
    
    NodeId id();
    
    String name();
    
    Map<String, Object> metadata();
    
    State execute(State inputState, ExecutionContext context, EventPublisher eventPublisher) throws Exception;
    
    default void onBeforeExecute(State state, ExecutionContext context) {
    }
    
    default void onAfterExecute(State inputState, State outputState, ExecutionContext context) {
    }
    
    default void onError(State state, ExecutionContext context, Throwable error) {
    }
    
    default boolean canExecute(State state, ExecutionContext context) {
        return true;
    }
}
