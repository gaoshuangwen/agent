package dev.langgraph.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileBasedCheckpointStoreTest {

    @TempDir
    Path tempDir;

    private FileBasedCheckpointStore store;

    @BeforeEach
    void setUp() throws PersistenceException {
        store = new FileBasedCheckpointStore(tempDir);
    }

    @AfterEach
    void tearDown() throws PersistenceException {
        store.clear();
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
        assertEquals(checkpoint.currentNodeId(), loaded.currentNodeId());
    }

    @Test
    void shouldPersistAcrossInstances() throws PersistenceException {
        Checkpoint checkpoint = createCheckpoint("cp-1", "graph-1", "exec-1");
        store.save(checkpoint);
        
        FileBasedCheckpointStore newStore = new FileBasedCheckpointStore(tempDir);
        
        assertTrue(newStore.exists("cp-1"));
        Checkpoint loaded = newStore.load("cp-1").orElseThrow();
        assertEquals(checkpoint.checkpointId(), loaded.checkpointId());
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
        
        store.clear();
        
        assertTrue(store.listByExecutionId("exec-1").isEmpty());
        assertTrue(store.listByExecutionId("exec-2").isEmpty());
    }

    @Test
    void shouldSerializeComplexState() throws PersistenceException {
        Map<String, Object> complexState = Map.of(
                "string", "value",
                "number", 42,
                "nested", Map.of("key", "value"),
                "list", List.of(1, 2, 3)
        );

        Checkpoint checkpoint = Checkpoint.builder()
                .checkpointId("cp-complex")
                .graphId("graph-1")
                .executionId("exec-1")
                .state(complexState)
                .currentNodeId("node-1")
                .timestamp(Instant.now())
                .build();

        store.save(checkpoint);
        
        Checkpoint loaded = store.load("cp-complex").orElseThrow();
        assertEquals("value", loaded.state().get("string"));
        assertEquals(42, loaded.state().get("number"));
        assertNotNull(loaded.state().get("nested"));
        assertNotNull(loaded.state().get("list"));
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
