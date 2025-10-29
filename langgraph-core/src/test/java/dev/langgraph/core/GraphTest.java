package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {

    @Test
    void shouldGetNodeById() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        assertTrue(graph.getNode(node1).isPresent());
        assertTrue(graph.getNode(node2).isPresent());
        assertFalse(graph.getNode(NodeId.of("non-existent")).isPresent());
    }

    @Test
    void shouldGetOutgoingEdges() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId node3 = NodeId.of("node3");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addFunctionalNode(node3, "Node3", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .addDirectEdge(node1, node3)
                .entryPoint(node1)
                .build();

        List<Edge> outgoing = graph.getOutgoingEdges(node1);
        assertEquals(2, outgoing.size());
    }

    @Test
    void shouldGetIncomingEdges() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId node3 = NodeId.of("node3");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addFunctionalNode(node3, "Node3", (state, ctx) -> state)
                .addDirectEdge(node1, node3)
                .addDirectEdge(node2, node3)
                .entryPoint(node1)
                .build();

        List<Edge> incoming = graph.getIncomingEdges(node3);
        assertEquals(2, incoming.size());
    }

    @Test
    void shouldSortOutgoingEdgesByPriority() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId node3 = NodeId.of("node3");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addFunctionalNode(node3, "Node3", (state, ctx) -> state)
                .addConditionalEdge(EdgeId.of("low"), node1, node2, (s, c) -> true, 1)
                .addConditionalEdge(EdgeId.of("high"), node1, node3, (s, c) -> true, 10)
                .entryPoint(node1)
                .build();

        List<Edge> outgoing = graph.getOutgoingEdges(node1);
        assertEquals(EdgeId.of("high"), outgoing.get(0).id());
        assertEquals(EdgeId.of("low"), outgoing.get(1).id());
    }

    @Test
    void shouldValidateSuccessfully() throws GraphValidationException {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        assertDoesNotThrow(graph::validate);
    }

    @Test
    void shouldFailValidationWithNoEntryPoint() {
        NodeId node1 = NodeId.of("node1");

        Graph graph = new DefaultGraph(
                GraphId.generate(),
                "TestGraph",
                null,
                Map.of(node1, NodeBuilder.newNode().id(node1).name("Node1").function((s, c) -> s).build()),
                List.of(),
                null
        );

        assertThrows(GraphValidationException.class, graph::validate);
    }

    @Test
    void shouldFailValidationWithUnreachableNodes() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId node3 = NodeId.of("node3");

        Graph graph = GraphBuilder.newGraph()
                .addFunctionalNode(node1, "Node1", (state, ctx) -> state)
                .addFunctionalNode(node2, "Node2", (state, ctx) -> state)
                .addFunctionalNode(node3, "Node3", (state, ctx) -> state)
                .addDirectEdge(node1, node2)
                .entryPoint(node1)
                .build();

        GraphValidationException exception = assertThrows(GraphValidationException.class, graph::validate);
        assertTrue(exception.getMessage().contains("Unreachable"));
    }

    @Test
    void shouldFailValidationWithInvalidEdgeSource() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId nonExistent = NodeId.of("non-existent");

        Edge invalidEdge = new DirectEdge(EdgeId.generate(), nonExistent, node2);

        Graph graph = new DefaultGraph(
                GraphId.generate(),
                "TestGraph",
                null,
                Map.of(
                        node1, NodeBuilder.newNode().id(node1).name("Node1").function((s, c) -> s).build(),
                        node2, NodeBuilder.newNode().id(node2).name("Node2").function((s, c) -> s).build()
                ),
                List.of(invalidEdge),
                node1
        );

        GraphValidationException exception = assertThrows(GraphValidationException.class, graph::validate);
        assertTrue(exception.getMessage().contains("source"));
    }

    @Test
    void shouldFailValidationWithInvalidEdgeTarget() {
        NodeId node1 = NodeId.of("node1");
        NodeId node2 = NodeId.of("node2");
        NodeId nonExistent = NodeId.of("non-existent");

        Edge invalidEdge = new DirectEdge(EdgeId.generate(), node1, nonExistent);

        Graph graph = new DefaultGraph(
                GraphId.generate(),
                "TestGraph",
                null,
                Map.of(
                        node1, NodeBuilder.newNode().id(node1).name("Node1").function((s, c) -> s).build(),
                        node2, NodeBuilder.newNode().id(node2).name("Node2").function((s, c) -> s).build()
                ),
                List.of(invalidEdge),
                node1
        );

        GraphValidationException exception = assertThrows(GraphValidationException.class, graph::validate);
        assertTrue(exception.getMessage().contains("target"));
    }
}
