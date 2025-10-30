package dev.langgraph.core.message;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void shouldCreateMessage() {
        Message message = Message.of("user", "Hello");
        
        assertNotNull(message.id());
        assertEquals("user", message.role());
        assertEquals("Hello", message.content());
        assertNotNull(message.timestamp());
        assertTrue(message.metadata().isEmpty());
    }

    @Test
    void shouldCreateUserMessage() {
        Message message = Message.user("User input");
        
        assertEquals("user", message.role());
        assertEquals("User input", message.content());
    }

    @Test
    void shouldCreateAssistantMessage() {
        Message message = Message.assistant("Assistant response");
        
        assertEquals("assistant", message.role());
        assertEquals("Assistant response", message.content());
    }

    @Test
    void shouldCreateSystemMessage() {
        Message message = Message.system("System instruction");
        
        assertEquals("system", message.role());
        assertEquals("System instruction", message.content());
    }

    @Test
    void shouldCreateMessageWithMetadata() {
        Map<String, Object> metadata = Map.of("source", "api", "priority", 1);
        Message message = Message.of("user", "Hello", metadata);
        
        assertEquals(2, message.metadata().size());
        assertEquals("api", message.metadata().get("source"));
        assertEquals(1, message.metadata().get("priority"));
    }

    @Test
    void shouldUpdateContent() {
        Message original = Message.user("Original");
        Message updated = original.withContent("Updated");
        
        assertEquals("Original", original.content());
        assertEquals("Updated", updated.content());
        assertEquals(original.id(), updated.id());
        assertEquals(original.role(), updated.role());
    }

    @Test
    void shouldAddMetadata() {
        Message original = Message.user("Hello");
        Message updated = original.withMetadata("key", "value");
        
        assertFalse(original.metadata().containsKey("key"));
        assertTrue(updated.metadata().containsKey("key"));
        assertEquals("value", updated.metadata().get("key"));
    }

    @Test
    void shouldGetMetadataWithDefault() {
        Message message = Message.of("user", "Hello", Map.of("existing", "value"));
        
        assertEquals("value", message.getMetadata("existing", "default"));
        assertEquals("default", message.getMetadata("missing", "default"));
    }
}
