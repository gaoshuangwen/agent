package dev.langgraph.core.execution;

import dev.langgraph.core.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotTest {

    @Test
    void shouldCreateStateSnapshot() {
        String executionId = "exec-123";
        NodeId nodeId = NodeId.of("node1");
        State state = State.of(java.util.Map.of("key", "value"));

        StateSnapshot snapshot = StateSnapshot.create(executionId, nodeId, state);

        assertNotNull(snapshot.snapshotId());
        assertEquals(executionId, snapshot.executionId());
        assertEquals(nodeId, snapshot.nodeId());
        assertEquals(state, snapshot.state());
        assertNotNull(snapshot.timestamp());
    }

    @Test
    void inMemorySnapshotStoreShouldSaveAndRetrieve() {
        SnapshotStore store = SnapshotStore.inMemory();
        String executionId = "exec-123";
        NodeId nodeId = NodeId.of("node1");
        State state = State.of(java.util.Map.of("key", "value"));

        StateSnapshot snapshot = StateSnapshot.create(executionId, nodeId, state);
        store.save(snapshot);

        assertEquals(snapshot, store.getLatest(executionId).orElseThrow());
    }

    @Test
    void inMemorySnapshotStoreShouldReturnAllSnapshots() {
        SnapshotStore store = SnapshotStore.inMemory();
        String executionId = "exec-123";

        StateSnapshot snapshot1 = StateSnapshot.create(executionId, NodeId.of("node1"), State.empty());
        StateSnapshot snapshot2 = StateSnapshot.create(executionId, NodeId.of("node2"), State.empty());

        store.save(snapshot1);
        store.save(snapshot2);

        List<StateSnapshot> snapshots = store.getAll(executionId);
        assertEquals(2, snapshots.size());
    }

    @Test
    void inMemorySnapshotStoreShouldReturnLatestSnapshot() {
        SnapshotStore store = SnapshotStore.inMemory();
        String executionId = "exec-123";

        StateSnapshot snapshot1 = StateSnapshot.create(executionId, NodeId.of("node1"), State.empty());
        StateSnapshot snapshot2 = StateSnapshot.create(executionId, NodeId.of("node2"), State.empty());

        store.save(snapshot1);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        store.save(snapshot2);

        StateSnapshot latest = store.getLatest(executionId).orElseThrow();
        assertEquals(snapshot2.snapshotId(), latest.snapshotId());
    }

    @Test
    void inMemorySnapshotStoreShouldClear() {
        SnapshotStore store = SnapshotStore.inMemory();
        String executionId = "exec-123";

        StateSnapshot snapshot = StateSnapshot.create(executionId, NodeId.of("node1"), State.empty());
        store.save(snapshot);

        store.clear(executionId);

        assertTrue(store.getLatest(executionId).isEmpty());
        assertTrue(store.getAll(executionId).isEmpty());
    }

    @Test
    void noOpSnapshotStoreShouldDoNothing() {
        SnapshotStore store = SnapshotStore.noOp();
        String executionId = "exec-123";

        StateSnapshot snapshot = StateSnapshot.create(executionId, NodeId.of("node1"), State.empty());
        store.save(snapshot);

        assertTrue(store.getLatest(executionId).isEmpty());
        assertTrue(store.getAll(executionId).isEmpty());
    }

    @Test
    void shouldCaptureStateInSnapshots() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph("Snapshots")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step1", true))
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state.with("step2", true))
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        SnapshotStore snapshotStore = SnapshotStore.inMemory();
        ExecutionConfig config = ExecutionConfig.builder()
                .enableSnapshots(true)
                .snapshotStore(snapshotStore)
                .build();

        StateGraph stateGraph = StateGraph.of(graph, config);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());

        List<StateSnapshot> snapshots = snapshotStore.getAll(result.executionId());
        assertEquals(2, snapshots.size());

        StateSnapshot firstSnapshot = snapshots.get(0);
        assertEquals(node1, firstSnapshot.nodeId());
        assertTrue(firstSnapshot.state().has("step1"));

        StateSnapshot secondSnapshot = snapshots.get(1);
        assertEquals(node2, secondSnapshot.nodeId());
        assertTrue(secondSnapshot.state().has("step1"));
        assertTrue(secondSnapshot.state().has("step2"));
        
        stateGraph.shutdown();
    }

    @Test
    void shouldNotCaptureSnapshotsWhenDisabled() {
        NodeId node1 = NodeId.of("node1");

        Graph graph = GraphBuilder.newGraph("NoSnapshots")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("done", true))
                .entryPoint(node1)
                .build();

        SnapshotStore snapshotStore = SnapshotStore.inMemory();
        ExecutionConfig config = ExecutionConfig.builder()
                .enableSnapshots(false)
                .snapshotStore(snapshotStore)
                .build();

        StateGraph stateGraph = StateGraph.of(graph, config);
        ExecutionResult result = stateGraph.execute(State.empty());

        assertTrue(result.isSuccess());
        assertTrue(snapshotStore.getAll(result.executionId()).isEmpty());
        
        stateGraph.shutdown();
    }
}
