package dev.langgraph.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JacksonStateSerializerTest {

    private JacksonStateSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new JacksonStateSerializer();
    }

    @Test
    void shouldSerializeAndDeserializeSimpleMap() throws SerializationException {
        Map<String, Object> original = Map.of("key", "value", "number", 42);
        
        byte[] serialized = serializer.serialize(original);
        Map<?, ?> deserialized = serializer.deserialize(serialized, Map.class);
        
        assertEquals("value", deserialized.get("key"));
        assertEquals(42, deserialized.get("number"));
    }

    @Test
    void shouldSerializeToString() throws SerializationException {
        Map<String, Object> data = Map.of("key", "value");
        
        String json = serializer.serializeToString(data);
        
        assertNotNull(json);
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }

    @Test
    void shouldDeserializeFromString() throws SerializationException {
        String json = "{\"key\":\"value\",\"number\":42}";
        
        Map<?, ?> deserialized = serializer.deserializeFromString(json, Map.class);
        
        assertEquals("value", deserialized.get("key"));
        assertEquals(42, deserialized.get("number"));
    }

    @Test
    void shouldSerializeComplexObjects() throws SerializationException {
        Map<String, Object> complex = Map.of(
                "string", "value",
                "number", 123,
                "nested", Map.of("inner", "value"),
                "list", List.of(1, 2, 3)
        );

        byte[] serialized = serializer.serialize(complex);
        Map<?, ?> deserialized = serializer.deserialize(serialized, Map.class);
        
        assertEquals("value", deserialized.get("string"));
        assertEquals(123, deserialized.get("number"));
        assertNotNull(deserialized.get("nested"));
        assertNotNull(deserialized.get("list"));
    }

    @Test
    void shouldHandleNullValues() throws SerializationException {
        Map<String, Object> withNull = new java.util.HashMap<>();
        withNull.put("key", null);
        withNull.put("other", "value");

        byte[] serialized = serializer.serialize(withNull);
        Map<?, ?> deserialized = serializer.deserialize(serialized, Map.class);
        
        assertTrue(deserialized.containsKey("key"));
        assertEquals("value", deserialized.get("other"));
    }

    @Test
    void shouldSerializeCheckpoint() throws SerializationException {
        Checkpoint checkpoint = Checkpoint.builder()
                .graphId("graph-1")
                .executionId("exec-1")
                .state(Map.of("key", "value"))
                .currentNodeId("node-1")
                .build();

        byte[] serialized = serializer.serialize(checkpoint);
        Checkpoint deserialized = serializer.deserialize(serialized, Checkpoint.class);
        
        assertEquals(checkpoint.checkpointId(), deserialized.checkpointId());
        assertEquals(checkpoint.graphId(), deserialized.graphId());
        assertEquals(checkpoint.executionId(), deserialized.executionId());
    }
}
