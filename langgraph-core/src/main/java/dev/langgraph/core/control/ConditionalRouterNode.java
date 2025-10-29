package dev.langgraph.core.control;

import dev.langgraph.core.*;

import java.util.*;
import java.util.function.BiFunction;

public final class ConditionalRouterNode extends AbstractNode {
    
    private final Map<String, GuardCondition> routes;
    private final String defaultRoute;

    public ConditionalRouterNode(NodeId id, String name, Map<String, Object> metadata,
                                Map<String, GuardCondition> routes, String defaultRoute) {
        super(id, name, metadata);
        this.routes = Collections.unmodifiableMap(new HashMap<>(routes));
        this.defaultRoute = defaultRoute;
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) {
        for (Map.Entry<String, GuardCondition> entry : routes.entrySet()) {
            if (entry.getValue().test(inputState, context)) {
                return inputState.with("__route__", entry.getKey());
            }
        }
        
        if (defaultRoute != null) {
            return inputState.with("__route__", defaultRoute);
        }
        
        throw new IllegalStateException("No matching route found and no default route specified");
    }

    public String getSelectedRoute(State state) {
        return state.get("__route__", "");
    }

    public static Builder builder(NodeId id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final Map<String, Object> metadata = new HashMap<>();
        private final Map<String, GuardCondition> routes = new LinkedHashMap<>();
        private String defaultRoute;

        private Builder(NodeId id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
        }

        public Builder route(String routeName, GuardCondition condition) {
            routes.put(routeName, condition);
            return this;
        }

        public Builder defaultRoute(String routeName) {
            this.defaultRoute = routeName;
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public ConditionalRouterNode build() {
            if (routes.isEmpty()) {
                throw new IllegalStateException("At least one route must be defined");
            }
            return new ConditionalRouterNode(id, name, metadata, routes, defaultRoute);
        }
    }
}
