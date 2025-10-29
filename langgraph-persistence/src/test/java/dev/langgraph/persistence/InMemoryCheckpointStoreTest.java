package dev.langgraph.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCheckpointStoreTest {

    private InMemoryCheckpointStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryCheckpointStore();
    }

    @Test
    void shouldSaveAndLoadCheckpoint() throws PersistenceException {
        Checkpoint checkpoint = createCheckpoint("cp-1", "graph-1", "exec-1");
        
        store.save(checkpoint);
        
        assertTrue(store.exists("cp-1"));
        Checkpoint loaded = store.load("cp-1").orElseThrow();
        assertEquals(checkpoint.checkpointId(), loaded.checkpointId());
        assertEquals(checkpoint.graphId(), loaded.graphId());
        assertEquals(checkpoint.executionId(), loaded.executionId());
    }

    @Test
    void shouldReturnEmptyForNonExistentCheckpoint() throws PersistenceException {
        assertTrue(store.load("non-existent").isEmpty());
    }

    @Test
    void shouldListCheckpointsByExecutionId() throws PersistenceException {
        store.save(createCheckpoint("cp-1", "graph-1", "exec-1"));
        store.save(createCheckpoint("cp-2", "graph-1", "exec-1"));
        store.save(createCheckpoint("cp-3", "graph-1", "exec-2"));

        List<Checkpoint> checkpoints = store.listByExecutionId("exec-1");
        
        assertEquals(2, checkpoints.size());
        assertTrue(checkpoints.stream().allMatch(cp -> cp.executionId().equals("exec-1")));
    }

    @Test
    void shouldListCheckpointsByGraphId() throws PersistenceException {
        store.save(createCheckpoint("cp-1", "graph-1", "exec-1"));
        store.save(createCheckpoint("cp-2", "graph-1", "exec-2"));
        store.save(createCheckpoint("cp-3", "graph-2", "exec-3"));

        List<Checkpoint> checkpoints = store.listByGraphId("graph-1");
        
        assertEquals(2, checkpoints.size());
        assertTrue(checkpoints.stream().allMatch(cp -> cp.graphId().equals("graph-1")));
    }

    @Test
    void shouldGetLatestCheckpoint() throws PersistenceException {
        Checkpoint cp1 = createCheckpoint("cp-1", "graph-1", "exec-1", Instant.now().minusSeconds(100));
        Checkpoint cp2 = createCheckpoint("cp-2", "graph-1", "exec-1", Instant.now().minusSeconds(50));
        Checkpoint cp3 = createCheckpoint("cp-3", "graph-1", "exec-1", Instant.now());

        store.save(cp1);
        store.save(cp2);
        store.save(cp3);

        Checkpoint latest = store.getLatestByExecutionId("exec-1").orElseThrow();
        assertEquals("cp-3", latest.checkpointId());
    }

    @Test
    void shouldDeleteCheckpoint() throws PersistenceException {
        Checkpoint checkpoint = createCheckpoint("cp-1", "graph-1", "exec-1");
        store.save(checkpoint);
        
        assertTrue(store.exists("cp-1"));
        
        store.delete("cp-1");
        
        assertFalse(store.exists("cp-1"));
        assertTrue(store.load("cp-1").isEmpty());
    }

    @Test
    void shouldDeleteByExecutionId() throws PersistenceException {
        store.save(createCheckpoint("cp-1", "graph-1", "exec-1"));
        store.save(createCheckpoint("cp-2", "graph-1", "exec-1"));
        store.save(createCheckpoint("cp-3", "graph-1", "exec-2"));

        store.deleteByExecutionId("exec-1");
        
        assertTrue(store.listByExecutionId("exec-1").isEmpty());
        assertEquals(1, store.listByExecutionId("exec-2").size());
    }

    @Test
    void shouldClearAll() throws PersistenceException {
        store.save(createCheckpoint("cp-1", "graph-1", "exec-1"));
        store.save(createCheckpoint("cp-2", "graph-1", "exec-2"));
        
        assertEquals(2, store.size());
        
        store.clear();
        
        assertEquals(0, store.size());
    }

    @Test
    void shouldSortCheckpointsByTimestamp() throws PersistenceException {
        Checkpoint cp3 = createCheckpoint("cp-3", "graph-1", "exec-1", Instant.now());
        Checkpoint cp1 = createCheckpoint("cp-1", "graph-1", "exec-1", Instant.now().minusSeconds(100));
        Checkpoint cp2 = createCheckpoint("cp-2", "graph-1", "exec-1", Instant.now().minusSeconds(50));

        store.save(cp3);
        store.save(cp1);
        store.save(cp2);

        List<Checkpoint> checkpoints = store.listByExecutionId("exec-1");
        
        assertEquals(3, checkpoints.size());
        assertEquals("cp-1", checkpoints.get(0).checkpointId());
        assertEquals("cp-2", checkpoints.get(1).checkpointId());
        assertEquals("cp-3", checkpoints.get(2).checkpointId());
    }

    private Checkpoint createCheckpoint(String checkpointId, String graphId, String executionId) {
        return createCheckpoint(checkpointId, graphId, executionId, Instant.now());
    }

    private Checkpoint createCheckpoint(String checkpointId, String graphId, String executionId, Instant timestamp) {
        return Checkpoint.builder()
                .checkpointId(checkpointId)
                .graphId(graphId)
                .executionId(executionId)
                .state(Map.of("key", "value"))
                .currentNodeId("node-1")
                .timestamp(timestamp)
                .build();
    }
}
