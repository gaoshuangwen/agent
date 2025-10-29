package dev.langgraph.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CompositeEventPublisher implements EventPublisher {
    
    private final List<EventListener> listeners;

    public CompositeEventPublisher() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public CompositeEventPublisher(List<EventListener> listeners) {
        this.listeners = new CopyOnWriteArrayList<>(Objects.requireNonNull(listeners));
    }

    public void addListener(EventListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public List<EventListener> getListeners() {
        return new ArrayList<>(listeners);
    }

    @Override
    public void publish(GraphEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Log but don't propagate listener errors
                System.err.println("Error in event listener: " + e.getMessage());
            }
        }
    }
}
