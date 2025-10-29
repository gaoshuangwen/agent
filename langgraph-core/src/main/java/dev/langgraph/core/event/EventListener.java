package dev.langgraph.core.event;

@FunctionalInterface
public interface EventListener {
    
    void onEvent(GraphEvent event);
}
