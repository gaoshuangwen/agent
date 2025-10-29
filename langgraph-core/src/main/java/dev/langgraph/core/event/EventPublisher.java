package dev.langgraph.core.event;

@FunctionalInterface
public interface EventPublisher {
    
    void publish(GraphEvent event);
    
    static EventPublisher noOp() {
        return event -> {};
    }
}
