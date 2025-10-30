package dev.langgraph.core.human;

import java.util.List;
import java.util.Optional;

public interface HumanTaskStore {
    
    void save(HumanTask task);
    
    Optional<HumanTask> load(String taskId);
    
    List<HumanTask> listByExecutionId(String executionId);
    
    List<HumanTask> listPendingTasks();
    
    List<HumanTask> listExpiredTasks();
    
    void delete(String taskId);
    
    void deleteByExecutionId(String executionId);
    
    void clear();
    
    boolean exists(String taskId);
}
