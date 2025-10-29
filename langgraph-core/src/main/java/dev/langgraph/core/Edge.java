package dev.langgraph.core;

import java.util.Map;

public interface Edge {
    
    EdgeId id();
    
    NodeId source();
    
    NodeId target();
    
    Map<String, Object> metadata();
    
    boolean canTraverse(State state, ExecutionContext context);
    
    default State transform(State state, ExecutionContext context) {
        return state;
    }
    
    default int priority() {
        return 0;
    }
}
