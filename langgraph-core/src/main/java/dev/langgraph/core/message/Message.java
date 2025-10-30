package dev.langgraph.core.message;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Message(
        String id,
        String role,
        String content,
        Instant timestamp,
        Map<String, Object> metadata
) {
    public Message {
        Objects.requireNonNull(id, "Message ID cannot be null");
        Objects.requireNonNull(role, "Message role cannot be null");
        Objects.requireNonNull(content, "Message content cannot be null");
        Objects.requireNonNull(timestamp, "Message timestamp cannot be null");
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public static Message of(String role, String content) {
        return new Message(UUID.randomUUID().toString(), role, content, Instant.now(), Map.of());
    }

    public static Message of(String role, String content, Map<String, Object> metadata) {
        return new Message(UUID.randomUUID().toString(), role, content, Instant.now(), metadata);
    }

    public static Message user(String content) {
        return of("user", content);
    }

    public static Message assistant(String content) {
        return of("assistant", content);
    }

    public static Message system(String content) {
        return of("system", content);
    }

    public Message withContent(String newContent) {
        return new Message(id, role, newContent, timestamp, metadata);
    }

    public Message withMetadata(String key, Object value) {
        Map<String, Object> newMetadata = new HashMap<>(metadata);
        newMetadata.put(key, value);
        return new Message(id, role, content, timestamp, newMetadata);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }

    @Override
    public String toString() {
        return "Message{role='" + role + "', content='" + 
               (content.length() > 50 ? content.substring(0, 47) + "..." : content) + "'}";
    }
}
