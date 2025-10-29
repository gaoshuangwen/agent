package dev.langgraph.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointRecoveryIntegrationTest {

    @TempDir
    Path tempDir;

    private CheckpointManager manager;

    @BeforeEach
    void setUp() throws PersistenceException {
        FileBasedCheckpointStore store = new FileBasedCheckpointStore(tempDir);
        manager = new CheckpointManager(store);
    }

    @Test
    void shouldRecoverFromInterruption() throws PersistenceException {
        String graphId = "workflow-graph";
        String executionId = "exec-001";

        Map<String, Object> initialState = new HashMap<>();
        initialState.put("step", 1);
        initialState.put("data", "processing");
        initialState.put("completed", List.of());

        Checkpoint cp1 = manager.createCheckpoint(graphId, executionId, initialState, "node-1");
        
        Map<String, Object> step2State = new HashMap<>(initialState);
        step2State.put("step", 2);
        step2State.put("data", "transformed");
        
        Checkpoint cp2 = manager.createCheckpoint(graphId, executionId, step2State, "node-2");

        Checkpoint latest = manager.getLatestCheckpoint(executionId).orElseThrow();
        
        assertEquals("node-2", latest.currentNodeId());
        assertEquals(2, latest.state().get("step"));
        assertEquals("transformed", latest.state().get("data"));
    }

    @Test
    void shouldSupportMultipleExecutions() throws PersistenceException {
        String graphId = "workflow-graph";

        manager.createCheckpoint(graphId, "exec-1", Map.of("value", 10), "node-A");
        manager.createCheckpoint(graphId, "exec-2", Map.of("value", 20), "node-B");
        manager.createCheckpoint(graphId, "exec-3", Map.of("value", 30), "node-C");

        Checkpoint exec1Latest = manager.getLatestCheckpoint("exec-1").orElseThrow();
        Checkpoint exec2Latest = manager.getLatestCheckpoint("exec-2").orElseThrow();
        Checkpoint exec3Latest = manager.getLatestCheckpoint("exec-3").orElseThrow();

        assertEquals(10, exec1Latest.state().get("value"));
        assertEquals(20, exec2Latest.state().get("value"));
        assertEquals(30, exec3Latest.state().get("value"));
    }

    @Test
    void shouldMaintainCheckpointHistory() throws Exception {
        String graphId = "graph-1";
        String executionId = "exec-1";

        manager.createCheckpoint(graphId, executionId, 
                Map.of("step", 1, "status", "init"), "node-1");
        Thread.sleep(10);
        
        manager.createCheckpoint(graphId, executionId, 
                Map.of("step", 2, "status", "processing"), "node-2");
        Thread.sleep(10);
        
        manager.createCheckpoint(graphId, executionId, 
                Map.of("step", 3, "status", "finalizing"), "node-3");
        Thread.sleep(10);
        
        manager.createCheckpoint(graphId, executionId, 
                Map.of("step", 4, "status", "completed"), "node-4");

        List<Checkpoint> history = manager.getCheckpointHistory(executionId);

        assertEquals(4, history.size());
        assertEquals(1, history.get(0).state().get("step"));
        assertEquals(2, history.get(1).state().get("step"));
        assertEquals(3, history.get(2).state().get("step"));
        assertEquals(4, history.get(3).state().get("step"));
    }

    @Test
    void shouldPreserveComplexState() throws PersistenceException {
        Map<String, Object> complexState = new HashMap<>();
        complexState.put("config", Map.of("timeout", 30, "retries", 3));
        complexState.put("results", List.of("result1", "result2", "result3"));
        complexState.put("metadata", Map.of(
                "startTime", System.currentTimeMillis(),
                "user", "test-user",
                "tags", List.of("tag1", "tag2")
        ));

        Checkpoint saved = manager.createCheckpoint(
                "graph-1", 
                "exec-1", 
                complexState, 
                "processing-node"
        );

        Checkpoint loaded = manager.loadCheckpoint(saved.checkpointId()).orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> loadedConfig = (Map<String, Object>) loaded.state().get("config");
        assertEquals(30, loadedConfig.get("timeout"));
        assertEquals(3, loadedConfig.get("retries"));

        @SuppressWarnings("unchecked")
        List<String> loadedResults = (List<String>) loaded.state().get("results");
        assertEquals(3, loadedResults.size());
        assertTrue(loadedResults.contains("result1"));
    }

    @Test
    void shouldSupportResumableWorkflow() throws Exception {
        String graphId = "resumable-workflow";
        String executionId = "exec-resumable";

        Map<String, Object> state = new HashMap<>();
        state.put("itemsProcessed", 0);
        state.put("totalItems", 100);
        state.put("currentBatch", 1);

        for (int i = 0; i < 5; i++) {
            state.put("itemsProcessed", i * 20);
            state.put("currentBatch", i + 1);
            
            manager.createCheckpoint(
                    graphId, 
                    executionId, 
                    new HashMap<>(state), 
                    "process-batch-" + (i + 1)
            );
            
            Thread.sleep(10);
        }

        assertTrue(manager.canResume(executionId));

        Checkpoint resumePoint = manager.getLatestCheckpoint(executionId).orElseThrow();
        
        assertEquals(80, resumePoint.state().get("itemsProcessed"));
        assertEquals(5, resumePoint.state().get("currentBatch"));
        assertEquals("process-batch-5", resumePoint.currentNodeId());
    }

    @Test
    void shouldHandleCheckpointDeletion() throws PersistenceException {
        String graphId = "cleanup-graph";
        String exec1 = "exec-1";
        String exec2 = "exec-2";

        manager.createCheckpoint(graphId, exec1, Map.of("data", "exec1"), "node-1");
        manager.createCheckpoint(graphId, exec2, Map.of("data", "exec2"), "node-1");

        assertTrue(manager.canResume(exec1));
        assertTrue(manager.canResume(exec2));

        manager.deleteExecutionCheckpoints(exec1);

        assertFalse(manager.canResume(exec1));
        assertTrue(manager.canResume(exec2));
    }

    @Test
    void shouldPersistAcrossManagerInstances() throws PersistenceException {
        String graphId = "persistent-graph";
        String executionId = "exec-persist";

        manager.createCheckpoint(graphId, executionId, 
                Map.of("value", "persisted"), "node-final");

        FileBasedCheckpointStore newStore = new FileBasedCheckpointStore(tempDir);
        CheckpointManager newManager = new CheckpointManager(newStore);

        assertTrue(newManager.canResume(executionId));
        
        Checkpoint recovered = newManager.getLatestCheckpoint(executionId).orElseThrow();
        assertEquals("persisted", recovered.state().get("value"));
        assertEquals("node-final", recovered.currentNodeId());
    }
}
