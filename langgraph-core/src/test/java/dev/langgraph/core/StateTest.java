package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {

    @Test
    void shouldCreateEmptyState() {
        State state = State.empty();
        
        assertNotNull(state);
        assertTrue(state.isEmpty());
        assertEquals(0, state.size());
    }

    @Test
    void shouldCreateStateWithData() {
        Map<String, Object> data = Map.of("key1", "value1", "key2", 42);
        State state = State.of(data);
        
        assertFalse(state.isEmpty());
        assertEquals(2, state.size());
        assertEquals(Optional.of("value1"), state.get("key1"));
        assertEquals(Optional.of(42), state.get("key2"));
    }

    @Test
    void shouldBeImmutable() {
        State original = State.of(Map.of("key", "value"));
        State modified = original.with("newKey", "newValue");
        
        assertNotSame(original, modified);
        assertFalse(original.has("newKey"));
        assertTrue(modified.has("newKey"));
        assertTrue(original.has("key"));
        assertTrue(modified.has("key"));
    }

    @Test
    void shouldAddValueWithWith() {
        State state = State.empty();
        State newState = state.with("key", "value");
        
        assertFalse(state.has("key"));
        assertTrue(newState.has("key"));
        assertEquals("value", newState.get("key", null));
    }

    @Test
    void shouldAddMultipleValuesWithWithAll() {
        State state = State.empty();
        State newState = state.withAll(Map.of("key1", "value1", "key2", "value2"));
        
        assertTrue(newState.has("key1"));
        assertTrue(newState.has("key2"));
        assertEquals(2, newState.size());
    }

    @Test
    void shouldRemoveValueWithWithout() {
        State state = State.of(Map.of("key1", "value1", "key2", "value2"));
        State newState = state.without("key1");
        
        assertTrue(state.has("key1"));
        assertFalse(newState.has("key1"));
        assertTrue(newState.has("key2"));
    }

    @Test
    void shouldGetValueWithDefault() {
        State state = State.of(Map.of("existing", "value"));
        
        assertEquals("value", state.get("existing", "default"));
        assertEquals("default", state.get("missing", "default"));
    }

    @Test
    void shouldReturnEmptyOptionalForMissingKey() {
        State state = State.of(Map.of("key", "value"));
        
        assertEquals(Optional.empty(), state.get("missing"));
    }

    @Test
    void shouldHandleNullDataInConstructor() {
        State state = State.of(null);
        
        assertNotNull(state);
        assertTrue(state.isEmpty());
    }

    @Test
    void shouldPreventExternalModification() {
        Map<String, Object> data = Map.of("key", "value");
        State state = State.of(data);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            state.data().put("newKey", "newValue");
        });
    }

    @Test
    void shouldSupportTypedAccess() {
        State state = State.of(Map.of("string", "text", "number", 42));
        
        Optional<String> stringValue = state.get("string");
        Optional<Integer> numberValue = state.get("number");
        
        assertTrue(stringValue.isPresent());
        assertTrue(numberValue.isPresent());
        assertEquals("text", stringValue.get());
        assertEquals(42, numberValue.get());
    }
}
