package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StateContainerTest {

    @Test
    void shouldCreateWithEmptyState() {
        StateContainer container = new StateContainer();
        
        assertNotNull(container.get());
        assertTrue(container.get().isEmpty());
    }

    @Test
    void shouldCreateWithInitialState() {
        State initial = State.of(Map.of("key", "value"));
        StateContainer container = new StateContainer(initial);
        
        assertEquals(initial, container.get());
    }

    @Test
    void shouldSetAndGetState() {
        StateContainer container = new StateContainer();
        State newState = State.of(Map.of("key", "value"));
        
        container.set(newState);
        
        assertEquals(newState, container.get());
    }

    @Test
    void shouldUpdateStateWithKeyValue() {
        StateContainer container = new StateContainer();
        
        State result = container.update("key", "value");
        
        assertTrue(result.has("key"));
        assertEquals("value", result.get("key", null));
        assertEquals(result, container.get());
    }

    @Test
    void shouldRemoveKeyFromState() {
        StateContainer container = new StateContainer(State.of(Map.of("key1", "value1", "key2", "value2")));
        
        State result = container.remove("key1");
        
        assertFalse(result.has("key1"));
        assertTrue(result.has("key2"));
        assertEquals(result, container.get());
    }

    @Test
    void shouldGetValueByKey() {
        StateContainer container = new StateContainer(State.of(Map.of("key", "value")));
        
        Optional<String> value = container.getValue("key");
        
        assertTrue(value.isPresent());
        assertEquals("value", value.get());
    }

    @Test
    void shouldCheckIfKeyExists() {
        StateContainer container = new StateContainer(State.of(Map.of("key", "value")));
        
        assertTrue(container.hasKey("key"));
        assertFalse(container.hasKey("missing"));
    }

    @Test
    void shouldClearState() {
        StateContainer container = new StateContainer(State.of(Map.of("key", "value")));
        
        container.clear();
        
        assertTrue(container.get().isEmpty());
    }

    @Test
    void shouldBeThreadSafe() throws InterruptedException {
        StateContainer container = new StateContainer();
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        container.update("thread-" + threadId, j);
                        container.get();
                        container.getValue("thread-" + threadId);
                    }
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        assertEquals(threadCount, successCount.get());
        assertNotNull(container.get());
    }

    @Test
    void shouldHandleNullStateInSet() {
        StateContainer container = new StateContainer();
        
        container.set(null);
        
        assertNotNull(container.get());
        assertTrue(container.get().isEmpty());
    }
}
