package dev.langgraph.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record State(Map<String, Object> data) {
    
    public State {
        data = data == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(data));
    }

    public static State empty() {
        return new State(Map.of());
    }

    public static State of(Map<String, Object> data) {
        return new State(data);
    }

    public State with(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.put(key, value);
        return new State(newData);
    }

    public State withAll(Map<String, Object> updates) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.putAll(updates);
        return new State(newData);
    }

    public State without(String key) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.remove(key);
        return new State(newData);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) data.get(key));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
