package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphBuilderTest {

    @Test
    void shouldBuildSimpleGraph() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph("TestGraph")
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state.with("step", 1))
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state.with("step", 2))
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        assertNotNull(graph);
        assertEquals("TestGraph", graph.name());
        assertEquals(2, graph.nodes().size());
        assertEquals(1, graph.edges().size());
        assertEquals(node1, graph.entryPoint().orElse(null));
    }

    @Test
    void shouldGenerateGraphIdIfNotProvided() {
        NodeId node1 = NodeId.of("node1");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .entryPoint(node1)
                .build();

        assertNotNull(graph.id());
    }

    @Test
    void shouldGenerateGraphNameIfNotProvided() {
        NodeId node1 = NodeId.of("node1");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .entryPoint(node1)
                .build();

        assertNotNull(graph.name());
        assertTrue(graph.name().startsWith("Graph-"));
    }

    @Test
    void shouldAddMetadata() {
        NodeId node1 = NodeId.of("node1");

        Graph graph = GraphBuilder.newGraph()
                .metadata("key1", "value1")
                .metadata("key2", "value2")
                .metadata(Map.of("key3", "value3"))
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .entryPoint(node1)
                .build();

        assertEquals(3, graph.metadata().size());
        assertEquals("value1", graph.metadata().get("key1"));
    }

    @Test
    void shouldAddNodesWithMetadata() {
        NodeId node1 = NodeId.of("node1");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", Map.of("meta", "data"), (state, ctx) -> state)
                .entryPoint(node1)
                .build();

        Node node = graph.getNode(node1).orElseThrow();
        assertEquals("data", node.metadata().get("meta"));
    }

    @Test
    void shouldAddConditionalEdges() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addConditionalEdge(node1, node2, (state, ctx) -> state.has("condition"))
                .entryPoint(node1)
                .build();

        assertEquals(1, graph.edges().size());
        assertTrue(graph.edges().get(0) instanceof ConditionalEdge);
    }

    @Test
    void shouldAddConditionalEdgesWithPriority() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        EdgeId edgeId = EdgeId.of("edge1");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addConditionalEdge(edgeId, node1, node2, (state, ctx) -> true, 10)
                .entryPoint(node1)
                .build();

        Edge edge = graph.edges().get(0);
        assertEquals(10, edge.priority());
    }

    @Test
    void shouldThrowExceptionIfNoNodes() {
        GraphBuilder builder = GraphBuilder.newGraph();

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void shouldThrowExceptionIfNoEntryPoint() {
        NodeId node1 = NodeId.of("node1");
        GraphBuilder builder = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void shouldThrowExceptionIfEntryPointDoesNotExist() {
        NodeId node1 = NodeId.of("node1");
        NodeId nonExistent = NodeId.of("non-existent");
        
        GraphBuilder builder = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .entryPoint(nonExistent);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void shouldThrowExceptionIfDuplicateNodeAdded() {
        NodeId node1 = NodeId.of("node1");
        Node node = NodeBuilder.newNode()
                .id(node1)
                .name("Node1")
                .function((state, ctx) -> state)
                .build();

        GraphBuilder builder = GraphBuilder.newGraph()
                .addNode(node);

        assertThrows(IllegalArgumentException.class, () -> builder.addNode(node));
    }

    @Test
    void shouldBuildAndValidateGraph() throws GraphValidationException {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .buildAndValidate();

        assertNotNull(graph);
    }

    @Test
    void shouldThrowValidationExceptionForUnreachableNodes() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        GraphBuilder builder = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .entryPoint(node1);

        assertThrows(GraphValidationException.class, builder::buildAndValidate);
    }

    @Test
    void shouldAddDirectEdgeWithGeneratedId() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        assertNotNull(graph.edges().get(0).id());
    }

    @Test
    void shouldAddConditionalEdgeWithGeneratedId() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addConditionalEdge(node1, node2, (state, ctx) -> true)
                .entryPoint(node1)
                .build();

        assertNotNull(graph.edges().get(0).id());
    }
}
