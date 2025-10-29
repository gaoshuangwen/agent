package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EdgeTest {

    @Test
    void shouldCreateDirectEdge() {
        NodeId source = NodeId.of("source");
        NodeId target = NodeId.of("target");
        EdgeId edgeId = EdgeId.of("edge");

        Edge edge = new DirectEdge(edgeId, source, target);

        assertEquals(edgeId, edge.id());
        assertEquals(source, edge.source());
        assertEquals(target, edge.target());
        assertTrue(edge.canTraverse(State.empty(), null));
    }

    @Test
    void shouldCreateConditionalEdge() {
        NodeId source = NodeId.of("source");
        NodeId target = NodeId.of("target");
        EdgeId edgeId = EdgeId.of("edge");

        Edge edge = new ConditionalEdge(
                edgeId, source, target, Map.of(),
                0, (state, ctx) -> state.has("condition")
        );

        assertEquals(edgeId, edge.id());
        assertEquals(source, edge.source());
        assertEquals(target, edge.target());
        
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        assertFalse(edge.canTraverse(State.empty(), context));
        assertTrue(edge.canTraverse(State.of(Map.of("condition", true)), context));
    }

    @Test
    void shouldSupportEdgePriority() {
        NodeId source = NodeId.of("source");
        NodeId target = NodeId.of("target");
        
        Edge lowPriority = new DirectEdge(EdgeId.of("low"), source, target, Map.of(), 1);
        Edge highPriority = new DirectEdge(EdgeId.of("high"), source, target, Map.of(), 10);

        assertEquals(1, lowPriority.priority());
        assertEquals(10, highPriority.priority());
    }

    @Test
    void shouldSupportEdgeMetadata() {
        NodeId source = NodeId.of("source");
        NodeId target = NodeId.of("target");
        Map<String, Object> metadata = Map.of("type", "conditional", "weight", 5);

        Edge edge = new DirectEdge(EdgeId.generate(), source, target, metadata, 0);

        assertEquals(2, edge.metadata().size());
        assertEquals("conditional", edge.metadata().get("type"));
        assertEquals(5, edge.metadata().get("weight"));
    }

    @Test
    void shouldTransformStateByDefault() {
        NodeId source = NodeId.of("source");
        NodeId target = NodeId.of("target");
        Edge edge = new DirectEdge(EdgeId.generate(), source, target);

        State input = State.of(Map.of("key", "value"));
        ExecutionContext context = ExecutionContext.create(GraphContext.of(GraphId.generate()));
        State output = edge.transform(input, context);

        assertEquals(input, output);
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        EdgeId id1 = EdgeId.of("edge1");
        EdgeId id2 = EdgeId.of("edge2");
        NodeId source = NodeId.of("source");
        NodeId target = NodeId.of("target");

        Edge edge1a = new DirectEdge(id1, source, target);
        Edge edge1b = new DirectEdge(id1, source, target);
        Edge edge2 = new DirectEdge(id2, source, target);

        assertEquals(edge1a, edge1b);
        assertEquals(edge1a.hashCode(), edge1b.hashCode());
        assertNotEquals(edge1a, edge2);
    }
}
