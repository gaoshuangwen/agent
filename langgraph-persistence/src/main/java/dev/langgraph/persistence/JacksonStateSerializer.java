package dev.langgraph.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Objects;

public final class JacksonStateSerializer implements StateSerializer {
    
    private final ObjectMapper objectMapper;

    public JacksonStateSerializer() {
        this(createDefaultObjectMapper());
    }

    public JacksonStateSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public byte[] serialize(Object state) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(state);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize state", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> type) throws SerializationException {
        try {
            return objectMapper.readValue(data, type);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize state", e);
        }
    }

    @Override
    public String serializeToString(Object state) throws SerializationException {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize state to string", e);
        }
    }

    @Override
    public <T> T deserializeFromString(String data, Class<T> type) throws SerializationException {
        try {
            return objectMapper.readValue(data, type);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize state from string", e);
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
