package dev.langgraph.persistence;

import java.util.List;
import java.util.Optional;

public interface CheckpointStore {
    
    void save(Checkpoint checkpoint) throws PersistenceException;
    
    Optional<Checkpoint> load(String checkpointId) throws PersistenceException;
    
    List<Checkpoint> listByExecutionId(String executionId) throws PersistenceException;
    
    List<Checkpoint> listByGraphId(String graphId) throws PersistenceException;
    
    Optional<Checkpoint> getLatestByExecutionId(String executionId) throws PersistenceException;
    
    void delete(String checkpointId) throws PersistenceException;
    
    void deleteByExecutionId(String executionId) throws PersistenceException;
    
    void clear() throws PersistenceException;
    
    boolean exists(String checkpointId) throws PersistenceException;
}
