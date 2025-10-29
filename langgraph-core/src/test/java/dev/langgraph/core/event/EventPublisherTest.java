package dev.langgraph.core.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventPublisherTest {

    @Test
    void shouldPublishEventsToMultipleListeners() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        List<GraphEvent> events1 = new ArrayList<>();
        List<GraphEvent> events2 = new ArrayList<>();

        publisher.addListener(events1::add);
        publisher.addListener(events2::add);

        GraphEvent event = new GraphEvent.NodeExecutionStarted("exec-1", "node-1", Instant.now(), Map.of());
        publisher.publish(event);

        assertEquals(1, events1.size());
        assertEquals(1, events2.size());
        assertEquals(event, events1.get(0));
        assertEquals(event, events2.get(0));
    }

    @Test
    void shouldRemoveListener() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        List<GraphEvent> events = new ArrayList<>();
        EventListener listener = events::add;

        publisher.addListener(listener);
        publisher.publish(new GraphEvent.NodeExecutionStarted("exec-1", "node-1", Instant.now(), Map.of()));
        assertEquals(1, events.size());

        publisher.removeListener(listener);
        publisher.publish(new GraphEvent.NodeExecutionStarted("exec-2", "node-2", Instant.now(), Map.of()));
        assertEquals(1, events.size());
    }

    @Test
    void shouldClearAllListeners() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        List<GraphEvent> events1 = new ArrayList<>();
        List<GraphEvent> events2 = new ArrayList<>();

        publisher.addListener(events1::add);
        publisher.addListener(events2::add);
        publisher.clearListeners();

        publisher.publish(new GraphEvent.NodeExecutionStarted("exec-1", "node-1", Instant.now(), Map.of()));

        assertEquals(0, events1.size());
        assertEquals(0, events2.size());
    }

    @Test
    void shouldHandleListenerExceptions() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        List<GraphEvent> events = new ArrayList<>();

        publisher.addListener(event -> {
            throw new RuntimeException("Listener error");
        });
        publisher.addListener(events::add);

        GraphEvent event = new GraphEvent.NodeExecutionStarted("exec-1", "node-1", Instant.now(), Map.of());
        
        assertDoesNotThrow(() -> publisher.publish(event));
        assertEquals(1, events.size());
    }

    @Test
    void shouldCreateNoOpPublisher() {
        EventPublisher noOp = EventPublisher.noOp();
        
        assertDoesNotThrow(() -> noOp.publish(
                new GraphEvent.NodeExecutionStarted("exec-1", "node-1", Instant.now(), Map.of())
        ));
    }

    @Test
    void shouldGetListeners() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();
        EventListener listener1 = event -> {};
        EventListener listener2 = event -> {};

        publisher.addListener(listener1);
        publisher.addListener(listener2);

        List<EventListener> listeners = publisher.getListeners();
        assertEquals(2, listeners.size());
    }

    @Test
    void shouldThrowExceptionForNullListener() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();

        assertThrows(NullPointerException.class, () -> publisher.addListener(null));
    }

    @Test
    void shouldThrowExceptionForNullEvent() {
        CompositeEventPublisher publisher = new CompositeEventPublisher();

        assertThrows(NullPointerException.class, () -> publisher.publish(null));
    }
}
