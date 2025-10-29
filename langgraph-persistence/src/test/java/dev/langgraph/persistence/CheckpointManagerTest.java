package dev.langgraph.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointManagerTest {

    private CheckpointManager manager;
    private InMemoryCheckpointStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryCheckpointStore();
        manager = new CheckpointManager(store);
    }

    @Test
    void shouldCreateCheckpoint() throws PersistenceException {
        Checkpoint checkpoint = manager.createCheckpoint(
                "graph-1", 
                "exec-1", 
                Map.of("key", "value"), 
                "node-1"
        );

        assertNotNull(checkpoint.checkpointId());
        assertEquals("graph-1", checkpoint.graphId());
        assertEquals("exec-1", checkpoint.executionId());
        assertEquals("node-1", checkpoint.currentNodeId());
        assertTrue(store.exists(checkpoint.checkpointId()));
    }

    @Test
    void shouldCreateCheckpointWithMetadata() throws PersistenceException {
        Map<String, Object> metadata = Map.of("phase", "init", "step", 1);
        
        Checkpoint checkpoint = manager.createCheckpoint(
                "graph-1",
                "exec-1",
                Map.of("key", "value"),
                "node-1",
                metadata
        );

        assertEquals(2, checkpoint.metadata().size());
        assertEquals("init", checkpoint.metadata().get("phase"));
        assertEquals(1, checkpoint.metadata().get("step"));
    }

    @Test
    void shouldLoadCheckpoint() throws PersistenceException {
        Checkpoint created = manager.createCheckpoint(
                "graph-1",
                "exec-1",
                Map.of("key", "value"),
                "node-1"
        );

        Checkpoint loaded = manager.loadCheckpoint(created.checkpointId()).orElseThrow();
        
        assertEquals(created.checkpointId(), loaded.checkpointId());
        assertEquals(created.graphId(), loaded.graphId());
        assertEquals(created.executionId(), loaded.executionId());
    }

    @Test
    void shouldGetLatestCheckpoint() throws Exception {
        manager.createCheckpoint("graph-1", "exec-1", Map.of("step", 1), "node-1");
        Thread.sleep(10);
        manager.createCheckpoint("graph-1", "exec-1", Map.of("step", 2), "node-2");
        Thread.sleep(10);
        Checkpoint latest = manager.createCheckpoint("graph-1", "exec-1", Map.of("step", 3), "node-3");

        Checkpoint retrieved = manager.getLatestCheckpoint("exec-1").orElseThrow();
        
        assertEquals(latest.checkpointId(), retrieved.checkpointId());
        assertEquals("node-3", retrieved.currentNodeId());
    }

    @Test
    void shouldGetCheckpointHistory() throws PersistenceException {
        manager.createCheckpoint("graph-1", "exec-1", Map.of("step", 1), "node-1");
        manager.createCheckpoint("graph-1", "exec-1", Map.of("step", 2), "node-2");
        manager.createCheckpoint("graph-1", "exec-1", Map.of("step", 3), "node-3");

        List<Checkpoint> history = manager.getCheckpointHistory("exec-1");
        
        assertEquals(3, history.size());
    }

    @Test
    void shouldCheckIfCanResume() throws PersistenceException {
        assertFalse(manager.canResume("exec-1"));

        manager.createCheckpoint("graph-1", "exec-1", Map.of(), "node-1");

        assertTrue(manager.canResume("exec-1"));
    }

    @Test
    void shouldDeleteCheckpoint() throws PersistenceException {
        Checkpoint checkpoint = manager.createCheckpoint(
                "graph-1",
                "exec-1",
                Map.of("key", "value"),
                "node-1"
        );

        assertTrue(manager.loadCheckpoint(checkpoint.checkpointId()).isPresent());

        manager.deleteCheckpoint(checkpoint.checkpointId());

        assertFalse(manager.loadCheckpoint(checkpoint.checkpointId()).isPresent());
    }

    @Test
    void shouldDeleteExecutionCheckpoints() throws PersistenceException {
        manager.createCheckpoint("graph-1", "exec-1", Map.of(), "node-1");
        manager.createCheckpoint("graph-1", "exec-1", Map.of(), "node-2");
        manager.createCheckpoint("graph-1", "exec-2", Map.of(), "node-1");

        assertTrue(manager.canResume("exec-1"));
        assertTrue(manager.canResume("exec-2"));

        manager.deleteExecutionCheckpoints("exec-1");

        assertFalse(manager.canResume("exec-1"));
        assertTrue(manager.canResume("exec-2"));
    }
}
