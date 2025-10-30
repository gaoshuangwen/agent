package dev.langgraph.core.control;

import dev.langgraph.core.State;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface StateMerger {
    
    State merge(List<State> states);
    
    static StateMerger first() {
        return states -> states.isEmpty() ? State.empty() : states.get(0);
    }
    
    static StateMerger last() {
        return states -> states.isEmpty() ? State.empty() : states.get(states.size() - 1);
    }
    
    static StateMerger combine() {
        return states -> {
            if (states.isEmpty()) {
                return State.empty();
            }
            Map<String, Object> combined = new HashMap<>();
            for (State state : states) {
                combined.putAll(state.data());
            }
            return State.of(combined);
        };
    }
    
    static StateMerger priorityMerge() {
        return states -> {
            if (states.isEmpty()) {
                return State.empty();
            }
            Map<String, Object> merged = new HashMap<>();
            for (int i = states.size() - 1; i >= 0; i--) {
                merged.putAll(states.get(i).data());
            }
            return State.of(merged);
        };
    }
    
    static StateMerger custom(StateMerger merger) {
        return merger;
    }
}
