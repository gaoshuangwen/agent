package dev.langgraph.core.human;

import dev.langgraph.core.ExecutionContext;
import dev.langgraph.core.State;

import java.util.Map;

@FunctionalInterface
public interface TimeoutStrategy {
    
    State onTimeout(HumanTask task, State currentState, ExecutionContext context);
    
    static TimeoutStrategy fail() {
        return (task, state, ctx) -> {
            throw new HumanTaskTimeoutException(
                    "Human task " + task.taskId() + " expired without response");
        };
    }
    
    static TimeoutStrategy continueWithDefault(Map<String, Object> defaultResponse) {
        return (task, state, ctx) -> {
            return state.with("__human_response__", defaultResponse)
                    .with("__human_task_timed_out__", true);
        };
    }
    
    static TimeoutStrategy escalate(String escalationNodeId) {
        return (task, state, ctx) -> {
            return state.with("__escalate_to__", escalationNodeId)
                    .with("__escalated_task__", task.taskId())
                    .with("__human_task_timed_out__", true);
        };
    }
    
    static TimeoutStrategy custom(TimeoutStrategy strategy) {
        return strategy;
    }
}
