package dev.langgraph.persistence;

public interface StateSerializer {
    
    byte[] serialize(Object state) throws SerializationException;
    
    <T> T deserialize(byte[] data, Class<T> type) throws SerializationException;
    
    String serializeToString(Object state) throws SerializationException;
    
    <T> T deserializeFromString(String data, Class<T> type) throws SerializationException;
}
