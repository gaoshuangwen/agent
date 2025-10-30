package dev.langgraph.integrations.langchain4j;

import dev.langgraph.core.*;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.*;
import java.util.function.BiFunction;

public final class ChainBuilder {
    
    private final GraphBuilder graphBuilder;
    private NodeId lastNodeId;
    private final List<NodeId> allNodes = new ArrayList<>();

    private ChainBuilder(String name) {
        this.graphBuilder = GraphBuilder.newGraph(name);
    }

    public static ChainBuilder newChain(String name) {
        return new ChainBuilder(name);
    }

    public ChainBuilder addLLMNode(String nodeIdStr, ChatLanguageModel model, String promptTemplate) {
        NodeId nodeId = NodeId.of(nodeIdStr);
        
        LLMNode llmNode = LLMNode.builder(nodeId, nodeIdStr, model)
                .prompt(promptTemplate)
                .build();
        
        graphBuilder.addNode(llmNode);
        allNodes.add(nodeId);
        
        if (lastNodeId != null) {
            graphBuilder.addDirectEdge(lastNodeId, nodeId);
        }
        
        lastNodeId = nodeId;
        return this;
    }

    public ChainBuilder addLLMNode(String nodeIdStr, ChatLanguageModel model, 
                                  PromptTemplate template, String outputKey) {
        NodeId nodeId = NodeId.of(nodeIdStr);
        
        LLMNode llmNode = LLMNode.builder(nodeId, nodeIdStr, model)
                .promptTemplate(template)
                .outputKey(outputKey)
                .build();
        
        graphBuilder.addNode(llmNode);
        allNodes.add(nodeId);
        
        if (lastNodeId != null) {
            graphBuilder.addDirectEdge(lastNodeId, nodeId);
        }
        
        lastNodeId = nodeId;
        return this;
    }

    public ChainBuilder addToolNode(String nodeIdStr, 
                                   Map<String, java.util.function.Function<Map<String, Object>, Object>> tools) {
        NodeId nodeId = NodeId.of(nodeIdStr);
        
        ToolNode.Builder builder = ToolNode.builder(nodeId, nodeIdStr);
        tools.forEach(builder::tool);
        
        graphBuilder.addNode(builder.build());
        allNodes.add(nodeId);
        
        if (lastNodeId != null) {
            graphBuilder.addDirectEdge(lastNodeId, nodeId);
        }
        
        lastNodeId = nodeId;
        return this;
    }

    public ChainBuilder addFunctionalNode(String nodeIdStr, String name,
                                         BiFunction<State, ExecutionContext, State> function) {
        NodeId nodeId = NodeId.of(nodeIdStr);
        
        graphBuilder.addFunctionalNode(nodeId, name, function);
        allNodes.add(nodeId);
        
        if (lastNodeId != null) {
            graphBuilder.addDirectEdge(lastNodeId, nodeId);
        }
        
        lastNodeId = nodeId;
        return this;
    }

    public ChainBuilder addConditionalEdge(String sourceIdStr, String targetIdStr,
                                          java.util.function.BiPredicate<State, ExecutionContext> condition) {
        graphBuilder.addConditionalEdge(
                NodeId.of(sourceIdStr),
                NodeId.of(targetIdStr),
                condition
        );
        return this;
    }

    public ChainBuilder withMetadata(String key, Object value) {
        graphBuilder.metadata(key, value);
        return this;
    }

    public Graph build() {
        if (!allNodes.isEmpty()) {
            graphBuilder.entryPoint(allNodes.get(0));
        }
        return graphBuilder.build();
    }

    public static AgentChainBuilder agentChain(String name) {
        return new AgentChainBuilder(name);
    }

    public static final class AgentChainBuilder {
        private final String name;
        private ChatLanguageModel model;
        private PromptTemplate systemPrompt;
        private final Map<String, java.util.function.Function<Map<String, Object>, Object>> tools = new HashMap<>();
        private int maxIterations = 5;

        private AgentChainBuilder(String name) {
            this.name = name;
        }

        public AgentChainBuilder model(ChatLanguageModel model) {
            this.model = model;
            return this;
        }

        public AgentChainBuilder systemPrompt(String prompt) {
            this.systemPrompt = PromptTemplate.of(prompt);
            return this;
        }

        public AgentChainBuilder systemPrompt(PromptTemplate template) {
            this.systemPrompt = template;
            return this;
        }

        public AgentChainBuilder tool(String name, java.util.function.Function<Map<String, Object>, Object> function) {
            this.tools.put(name, function);
            return this;
        }

        public AgentChainBuilder maxIterations(int max) {
            this.maxIterations = max;
            return this;
        }

        public Graph build() {
            if (model == null) {
                throw new IllegalStateException("Model must be set");
            }

            ChainBuilder chain = newChain(name);

            if (systemPrompt != null) {
                chain.addFunctionalNode("system", "System",
                        (state, ctx) -> state.with("system_prompt", systemPrompt.render(state.data())));
            }

            chain.addLLMNode("agent", model, 
                    systemPrompt != null ? "{{system_prompt}}\n\nUser: {{input}}" : "{{input}}",
                    "agent_response");

            if (!tools.isEmpty()) {
                chain.addToolNode("tools", tools);
                
                chain.addConditionalEdge("agent", "tools",
                        (state, ctx) -> {
                            String response = state.get("agent_response", "").toString();
                            return response.contains("TOOL:");
                        });
            }

            return chain.build();
        }
    }
}
