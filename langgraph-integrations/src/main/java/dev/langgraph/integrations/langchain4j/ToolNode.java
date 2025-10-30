package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.*;
import java.util.function.Function;

public final class ToolNode extends AbstractNode {
    
    private final Map<String, Function<Map<String, Object>, Object>> tools;
    private final String inputKey;
    private final String outputKey;

    public ToolNode(NodeId id, String name, Map<String, Object> metadata,
                   Map<String, Function<Map<String, Object>, Object>> tools,
                   String inputKey, String outputKey) {
        super(id, name, metadata);
        this.tools = Map.copyOf(Objects.requireNonNull(tools, "Tools cannot be null"));
        this.inputKey = Objects.requireNonNull(inputKey, "Input key cannot be null");
        this.outputKey = Objects.requireNonNull(outputKey, "Output key cannot be null");
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> toolRequest = (Map<String, Object>) inputState.get(inputKey);
        
        if (toolRequest == null) {
            throw new IllegalStateException("No tool request found at key: " + inputKey);
        }

        String toolName = (String) toolRequest.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) toolRequest.getOrDefault("arguments", Map.of());

        if (!tools.containsKey(toolName)) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }

        Object result = tools.get(toolName).apply(arguments);

        return inputState
                .with(outputKey, result)
                .with("__tool_name__", toolName)
                .with("__tool_arguments__", arguments);
    }

    public static Builder builder(NodeId id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final Map<String, Object> metadata = new HashMap<>();
        private final Map<String, Function<Map<String, Object>, Object>> tools = new HashMap<>();
        private String inputKey = "tool_request";
        private String outputKey = "tool_result";

        private Builder(NodeId id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
        }

        public Builder tool(String name, Function<Map<String, Object>, Object> function) {
            this.tools.put(name, function);
            return this;
        }

        public Builder inputKey(String inputKey) {
            this.inputKey = inputKey;
            return this;
        }

        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public ToolNode build() {
            if (tools.isEmpty()) {
                throw new IllegalStateException("At least one tool must be registered");
            }
            return new ToolNode(id, name, metadata, tools, inputKey, outputKey);
        }
    }
}
