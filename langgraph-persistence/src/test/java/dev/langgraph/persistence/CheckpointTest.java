package dev.langgraph.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointTest {

    @Test
    void shouldCreateCheckpoint() {
        Checkpoint checkpoint = Checkpoint.builder()
                .checkpointId("cp-1")
                .graphId("graph-1")
                .executionId("exec-1")
                .state(Map.of("key", "value"))
                .currentNodeId("node-1")
                .timestamp(Instant.now())
                .build();

        assertEquals("cp-1", checkpoint.checkpointId());
        assertEquals("graph-1", checkpoint.graphId());
        assertEquals("exec-1", checkpoint.executionId());
        assertEquals("node-1", checkpoint.currentNodeId());
        assertTrue(checkpoint.state().containsKey("key"));
        assertNotNull(checkpoint.timestamp());
    }

    @Test
    void shouldGenerateCheckpointId() {
        Checkpoint checkpoint = Checkpoint.builder()
                .graphId("graph-1")
                .executionId("exec-1")
                .state(Map.of())
                .build();

        assertNotNull(checkpoint.checkpointId());
        assertFalse(checkpoint.checkpointId().isEmpty());
    }

    @Test
    void shouldGenerateTimestamp() {
        Checkpoint checkpoint = Checkpoint.builder()
                .graphId("graph-1")
                .executionId("exec-1")
                .state(Map.of())
                .build();

        assertNotNull(checkpoint.timestamp());
    }

    @Test
    void shouldAddMetadata() {
        Checkpoint checkpoint = Checkpoint.builder()
                .graphId("graph-1")
                .executionId("exec-1")
                .state(Map.of())
                .addMetadata("key1", "value1")
                .addMetadata("key2", 42)
                .build();

        assertEquals(2, checkpoint.metadata().size());
        assertEquals("value1", checkpoint.metadata().get("key1"));
        assertEquals(42, checkpoint.metadata().get("key2"));
    }

    @Test
    void shouldBeImmutable() {
        Map<String, Object> state = new java.util.HashMap<>();
        state.put("key", "value");

        Checkpoint checkpoint = Checkpoint.builder()
                .graphId("graph-1")
                .executionId("exec-1")
                .state(state)
                .build();

        assertThrows(UnsupportedOperationException.class, () -> {
            checkpoint.state().put("newKey", "newValue");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            checkpoint.metadata().put("newKey", "newValue");
        });
    }

    @Test
    void shouldRequireGraphId() {
        assertThrows(NullPointerException.class, () -> {
            Checkpoint.builder()
                    .executionId("exec-1")
                    .state(Map.of())
                    .build();
        });
    }

    @Test
    void shouldRequireExecutionId() {
        assertThrows(NullPointerException.class, () -> {
            Checkpoint.builder()
                    .graphId("graph-1")
                    .state(Map.of())
                    .build();
        });
    }
}
