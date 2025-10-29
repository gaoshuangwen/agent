package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeBuilderTest {

    @Test
    void shouldBuildNodeWithAllProperties() {
        NodeId id = NodeId.of("test-node");
        Node node = NodeBuilder.newNode()
                .id(id)
                .name("TestNode")
                .metadata("key", "value")
                .function((state, ctx) -> state.with("result", "success"))
                .build();

        assertNotNull(node);
        assertEquals(id, node.id());
        assertEquals("TestNode", node.name());
        assertEquals("value", node.metadata().get("key"));
    }

    @Test
    void shouldGenerateIdIfNotProvided() {
        Node node = NodeBuilder.newNode()
                .name("TestNode")
                .function((state, ctx) -> state)
                .build();

        assertNotNull(node.id());
    }

    @Test
    void shouldGenerateNameIfNotProvided() {
        Node node = NodeBuilder.newNode()
                .function((state, ctx) -> state)
                .build();

        assertNotNull(node.name());
        assertTrue(node.name().startsWith("Node-"));
    }

    @Test
    void shouldAcceptMultipleMetadataEntries() {
        Node node = NodeBuilder.newNode()
                .metadata("key1", "value1")
                .metadata("key2", "value2")
                .metadata(Map.of("key3", "value3"))
                .function((state, ctx) -> state)
                .build();

        assertEquals(3, node.metadata().size());
        assertEquals("value1", node.metadata().get("key1"));
        assertEquals("value2", node.metadata().get("key2"));
        assertEquals("value3", node.metadata().get("key3"));
    }

    @Test
    void shouldThrowExceptionIfFunctionNotSet() {
        NodeBuilder builder = NodeBuilder.newNode().name("TestNode");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void shouldCreateBuilderWithName() {
        Node node = NodeBuilder.newNode("MyNode")
                .function((state, ctx) -> state)
                .build();

        assertEquals("MyNode", node.name());
    }

    @Test
    void shouldThrowExceptionForNullId() {
        NodeBuilder builder = NodeBuilder.newNode();

        assertThrows(NullPointerException.class, () -> builder.id(null));
    }

    @Test
    void shouldThrowExceptionForNullName() {
        NodeBuilder builder = NodeBuilder.newNode();

        assertThrows(NullPointerException.class, () -> builder.name(null));
    }

    @Test
    void shouldThrowExceptionForNullFunction() {
        NodeBuilder builder = NodeBuilder.newNode();

        assertThrows(NullPointerException.class, () -> builder.function(null));
    }

    @Test
    void shouldThrowExceptionForNullMetadataKey() {
        NodeBuilder builder = NodeBuilder.newNode();

        assertThrows(NullPointerException.class, () -> builder.metadata(null, "value"));
    }
}
