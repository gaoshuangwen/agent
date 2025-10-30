package dev.langgraph.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphIdTest {

    @Test
    void shouldCreateGraphIdWithValue() {
        GraphId id = GraphId.of("test-id");
        assertEquals("test-id", id.value());
    }

    @Test
    void shouldGenerateUniqueGraphIds() {
        GraphId id1 = GraphId.generate();
        GraphId id2 = GraphId.generate();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldThrowExceptionForNullValue() {
        assertThrows(NullPointerException.class, () -> GraphId.of(null));
    }

    @Test
    void shouldThrowExceptionForBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> GraphId.of(""));
        assertThrows(IllegalArgumentException.class, () -> GraphId.of("   "));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        GraphId id1 = GraphId.of("same-id");
        GraphId id2 = GraphId.of("same-id");
        GraphId id3 = GraphId.of("different-id");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    void shouldConvertToString() {
        GraphId id = GraphId.of("test-id");
        assertEquals("test-id", id.toString());
    }
}
