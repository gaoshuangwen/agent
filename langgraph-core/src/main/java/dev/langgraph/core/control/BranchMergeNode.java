package dev.langgraph.core.control;

import dev.langgraph.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BranchMergeNode extends AbstractNode {
    
    private final StateMerger merger;

    public BranchMergeNode(NodeId id, String name, Map<String, Object> metadata, StateMerger merger) {
        super(id, name, metadata);
        this.merger = Objects.requireNonNull(merger, "Merger cannot be null");
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        @SuppressWarnings("unchecked")
        List<State> branchStates = inputState.get("__branch_states__", List.of());
        
        if (branchStates.isEmpty()) {
            return inputState;
        }

        State mergedState = merger.merge(branchStates);
        return mergedState.without("__branch_states__");
    }

    public static Builder builder(NodeId id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final Map<String, Object> metadata = new java.util.HashMap<>();
        private StateMerger merger = StateMerger.combine();

        private Builder(NodeId id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
        }

        public Builder merger(StateMerger merger) {
            this.merger = Objects.requireNonNull(merger);
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public BranchMergeNode build() {
            return new BranchMergeNode(id, name, metadata, merger);
        }
    }
}
