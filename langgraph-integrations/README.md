# LangGraph LangChain4j Integration

This module provides seamless integration between LangGraph and LangChain4j, enabling you to build sophisticated AI agents with LLM capabilities, tool orchestration, and prompt templating.

## Features

- **LLM Nodes**: Direct integration with LangChain4j chat models
- **Tool Execution**: Function calling and tool orchestration
- **Prompt Templating**: Flexible template system with variable substitution
- **Chain Composition**: Fluent API for building multi-step LLM workflows
- **Streaming Support**: Handle streaming responses from LLMs
- **Configuration Management**: Model provider abstraction with sensible defaults

## Core Components

### LLMConfig

Configuration abstraction for LLM model settings:

```java
LLMConfig config = LLMConfig.builder("gpt-4")
        .temperature(0.7)
        .maxTokens(2048)
        .timeout(Duration.ofSeconds(30))
        .parameter("top_p", 0.9)
        .build();

// Or use presets
LLMConfig gpt4 = LLMConfig.gpt4();
LLMConfig gpt35 = LLMConfig.gpt35Turbo();
```

### PromptTemplate

Template system with variable substitution:

```java
PromptTemplate template = PromptTemplate.of(
        "Hello {{name}}, you are {{age}} years old"
);

String rendered = template.render(Map.of(
        "name", "Alice",
        "age", 30
));
// Output: "Hello Alice, you are 30 years old"

// With defaults
PromptTemplate withDefaults = PromptTemplate.of(
        "Welcome {{user}} to {{location}}!",
        Map.of("location", "Earth")
);

String result = withDefaults.render(Map.of("user", "Bob"));
// Output: "Welcome Bob to Earth!"
```

**Message Templates**:
```java
PromptTemplate system = PromptTemplate.systemMessage("You are a helpful assistant");
PromptTemplate user = PromptTemplate.userMessage("Hello!");
PromptTemplate assistant = PromptTemplate.assistantMessage("Hi there!");
```

### LLMNode

Integrate LLM calls into your graph:

```java
ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4")
        .build();

LLMNode llmNode = LLMNode.builder(NodeId.of("llm"), "LLM", model)
        .prompt("What is the capital of {{country}}?")
        .outputKey("answer")
        .config(LLMConfig.gpt4())
        .build();

// Use in graph
Graph graph = GraphBuilder.newGraph("MyGraph")
        .addNode(llmNode)
        .entryPoint(NodeId.of("llm"))
        .build();

StateGraph stateGraph = StateGraph.of(graph);
ExecutionResult result = stateGraph.execute(
        State.empty().with("country", "France")
);

String answer = result.finalState().get("answer", "");
// Output: "The capital of France is Paris."
```

**Output State Keys**:
- `{outputKey}`: The LLM response (default: `llm_response`)
- `__llm_prompt__`: The rendered prompt sent to the LLM
- `__llm_model__`: The model name used

### ToolNode

Execute tools/functions based on LLM decisions:

```java
ToolNode toolNode = ToolNode.builder(NodeId.of("tools"), "Tools")
        .tool("add", args -> {
            int a = (int) args.get("a");
            int b = (int) args.get("b");
            return a + b;
        })
        .tool("multiply", args -> {
            int a = (int) args.get("a");
            int b = (int) args.get("b");
            return a * b;
        })
        .tool("get_weather", args -> {
            String city = (String) args.get("city");
            return "Sunny, 72°F in " + city;
        })
        .inputKey("tool_request")
        .outputKey("tool_result")
        .build();

// Use in state
State state = State.empty()
        .with("tool_request", Map.of(
                "tool", "add",
                "arguments", Map.of("a", 5, "b", 3)
        ));

State result = toolNode.execute(state, context, eventPublisher);
// result.get("tool_result") == 8
```

**Output State Keys**:
- `{outputKey}`: The tool execution result (default: `tool_result`)
- `__tool_name__`: The name of the executed tool
- `__tool_arguments__`: The arguments passed to the tool

### ChainBuilder

Fluent API for composing LLM workflows:

```java
Graph graph = ChainBuilder.newChain("MyChain")
        .addLLMNode("summarize", model, 
                "Summarize this text: {{text}}",
                "summary")
        .addLLMNode("translate", model,
                "Translate to French: {{summary}}",
                "translation")
        .addFunctionalNode("format", "Format",
                (state, ctx) -> state.with("formatted", 
                        "Result: " + state.get("translation")))
        .build();

StateGraph stateGraph = StateGraph.of(graph);
ExecutionResult result = stateGraph.execute(
        State.empty().with("text", "Long text here...")
);
```

**Chaining Pattern**: Each node added is automatically connected to the previous node with a direct edge.

### Agent Chain Builder

Simplified API for building AI agents with tools:

```java
Graph agentGraph = ChainBuilder.agentChain("MyAgent")
        .model(model)
        .systemPrompt("You are a helpful assistant with access to tools.")
        .tool("get_time", args -> Instant.now().toString())
        .tool("get_weather", args -> "Sunny, 72°F")
        .tool("calculate", args -> {
            String operation = (String) args.get("op");
            int a = (int) args.get("a");
            int b = (int) args.get("b");
            return switch (operation) {
                case "add" -> a + b;
                case "multiply" -> a * b;
                default -> throw new IllegalArgumentException("Unknown operation");
            };
        })
        .maxIterations(5)
        .build();

StateGraph stateGraph = StateGraph.of(agentGraph);
ExecutionResult result = stateGraph.execute(
        State.empty().with("input", "What time is it?")
);
```

### StreamingLLMNode

Handle streaming responses from LLMs:

```java
StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4")
        .build();

StreamingLLMNode streamingNode = StreamingLLMNode.builder(
                NodeId.of("streaming"), "Streaming LLM", streamingModel)
        .prompt("Tell me a story about {{topic}}")
        .onToken(token -> System.out.print(token)) // Print tokens as they arrive
        .outputKey("story")
        .build();
```

**Token Handler**: The `onToken` callback is invoked for each token as it's received.

## Complete Examples

### Simple Q&A System

```java
ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-3.5-turbo")
        .build();

Graph qaGraph = ChainBuilder.newChain("QA")
        .addLLMNode("answer", model, 
                "Answer this question: {{question}}")
        .build();

StateGraph stateGraph = StateGraph.of(qaGraph);
ExecutionResult result = stateGraph.execute(
        State.empty().with("question", "What is machine learning?")
);

System.out.println(result.finalState().get("llm_response"));
```

### Multi-Step Analysis

```java
Graph analysisGraph = ChainBuilder.newChain("Analysis")
        .addLLMNode("extract_key_points", model,
                "Extract key points from: {{text}}",
                "key_points")
        .addLLMNode("analyze_sentiment", model,
                "Analyze sentiment of these key points: {{key_points}}",
                "sentiment")
        .addLLMNode("generate_summary", model,
                "Generate a summary based on key points: {{key_points}} and sentiment: {{sentiment}}",
                "final_summary")
        .build();

StateGraph stateGraph = StateGraph.of(analysisGraph);
ExecutionResult result = stateGraph.execute(
        State.empty().with("text", "Customer feedback text...")
);
```

### Tool-Based Agent

```java
Graph toolAgent = ChainBuilder.newChain("ToolAgent")
        .addLLMNode("plan", model,
                PromptTemplate.of("""
                        System: You are a helpful assistant with access to tools.
                        User: {{user_request}}
                        
                        Available tools:
                        - search: Search the web
                        - calculate: Perform calculations
                        - translate: Translate text
                        
                        What tool should be used? Respond with: TOOL: <name> ARGS: <args>
                        """),
                "plan")
        .addFunctionalNode("parse_tool_call", "Parse",
                (state, ctx) -> {
                    String plan = state.get("plan", "").toString();
                    // Parse tool name and arguments from plan
                    // ... parsing logic ...
                    return state.with("tool_request", Map.of(
                            "tool", "search",
                            "arguments", Map.of("query", "weather")
                    ));
                })
        .addToolNode("execute_tool", Map.of(
                "search", args -> "Search results for: " + args.get("query"),
                "calculate", args -> performCalculation(args),
                "translate", args -> translateText(args)
        ))
        .addLLMNode("respond", model,
                "Based on the tool result: {{tool_result}}, respond to the user request: {{user_request}}",
                "response")
        .build();

StateGraph stateGraph = StateGraph.of(toolAgent);
ExecutionResult result = stateGraph.execute(
        State.empty().with("user_request", "Search for weather in New York")
);
```

### Conversational Agent with History

```java
PromptTemplate conversationalTemplate = PromptTemplate.builder()
        .template("""
                System: You are a helpful conversational assistant.
                
                Conversation History:
                {{history}}
                
                User: {{user_message}}
                
                Assistant:
                """)
        .defaultVariable("history", "")
        .build();

Graph conversationalGraph = ChainBuilder.newChain("Conversational")
        .addFunctionalNode("update_history", "UpdateHistory",
                (state, ctx) -> {
                    String history = state.get("history", "").toString();
                    String userMsg = state.get("user_message", "").toString();
                    String newHistory = history + "\nUser: " + userMsg;
                    return state.with("history", newHistory);
                })
        .addLLMNode("respond", model, conversationalTemplate, "response")
        .addFunctionalNode("save_response", "SaveResponse",
                (state, ctx) -> {
                    String history = state.get("history", "").toString();
                    String response = state.get("response", "").toString();
                    String updatedHistory = history + "\nAssistant: " + response;
                    return state.with("history", updatedHistory);
                })
        .build();
```

### Conditional Routing Based on LLM Classification

```java
Graph conditionalGraph = ChainBuilder.newChain("Conditional")
        .addLLMNode("classify", model,
                "Classify this text as 'question', 'command', or 'statement': {{input}}",
                "classification")
        .addFunctionalNode("route", "Route",
                (state, ctx) -> {
                    String classification = state.get("classification", "").toString();
                    return state.with("route_to", classification);
                })
        .addLLMNode("handle_question", model,
                "Answer this question: {{input}}",
                "response")
        .addLLMNode("handle_command", model,
                "Execute this command: {{input}}",
                "response")
        .addLLMNode("handle_statement", model,
                "Acknowledge this statement: {{input}}",
                "response")
        .build();

// Add conditional edges in GraphBuilder
GraphBuilder builder = GraphBuilder.fromGraph(conditionalGraph);
builder.addConditionalEdge(NodeId.of("route"), NodeId.of("handle_question"),
        (state, ctx) -> state.get("route_to", "").equals("question"));
builder.addConditionalEdge(NodeId.of("route"), NodeId.of("handle_command"),
        (state, ctx) -> state.get("route_to", "").equals("command"));
builder.addConditionalEdge(NodeId.of("route"), NodeId.of("handle_statement"),
        (state, ctx) -> state.get("route_to", "").equals("statement"));
```

## Model Provider Configuration

### OpenAI

```java
import dev.langchain4j.model.openai.OpenAiChatModel;

ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4")
        .temperature(0.7)
        .maxTokens(2000)
        .timeout(Duration.ofSeconds(60))
        .build();
```

### Azure OpenAI

```java
import dev.langchain4j.model.azure.AzureOpenAiChatModel;

ChatLanguageModel model = AzureOpenAiChatModel.builder()
        .apiKey(System.getenv("AZURE_OPENAI_KEY"))
        .endpoint(System.getenv("AZURE_OPENAI_ENDPOINT"))
        .deploymentName("gpt-4")
        .temperature(0.7)
        .build();
```

### Anthropic Claude

```java
import dev.langchain4j.model.anthropic.AnthropicChatModel;

ChatLanguageModel model = AnthropicChatModel.builder()
        .apiKey(System.getenv("ANTHROPIC_API_KEY"))
        .modelName("claude-3-opus")
        .temperature(0.7)
        .build();
```

## Error Handling

All nodes follow standard LangGraph error handling:

```java
try {
    ExecutionResult result = stateGraph.execute(state);
    if (result.isSuccess()) {
        // Handle success
    } else {
        // Handle failure
        Throwable error = result.error();
    }
} catch (Exception e) {
    // Handle exception
}
```

## Best Practices

### 1. Use Descriptive Node Names

```java
.addLLMNode("extract_entities", model, ...)
.addLLMNode("classify_intent", model, ...)
.addLLMNode("generate_response", model, ...)
```

### 2. Set Appropriate Output Keys

```java
.addLLMNode("summarize", model, template, "summary")
.addLLMNode("translate", model, template, "translation")
```

### 3. Use Prompt Templates Consistently

```java
PromptTemplate systemTemplate = PromptTemplate.builder()
        .template("""
                System: {{system_instruction}}
                User: {{user_input}}
                """)
        .defaultVariable("system_instruction", "You are a helpful assistant")
        .build();
```

### 4. Handle Tool Errors Gracefully

```java
.tool("api_call", args -> {
    try {
        return callExternalAPI(args);
    } catch (Exception e) {
        return Map.of("error", e.getMessage());
    }
})
```

### 5. Configure Timeouts Appropriately

```java
LLMConfig config = LLMConfig.builder("gpt-4")
        .timeout(Duration.ofSeconds(30)) // Shorter for simple tasks
        .build();
```

### 6. Use Streaming for Long Responses

```java
StreamingLLMNode.builder(id, name, streamingModel)
        .onToken(token -> {
            // Send to client in real-time
            websocket.send(token);
        })
        .build();
```

## Testing

Use mocked models for testing:

```java
@Mock
private ChatLanguageModel mockModel;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockModel.generate(anyString())).thenReturn("Mocked response");
}

@Test
void testLLMNode() throws Exception {
    LLMNode node = LLMNode.builder(NodeId.of("test"), "Test", mockModel)
            .prompt("Test prompt")
            .build();
    
    State result = node.execute(State.empty(), context, EventPublisher.noOp());
    
    assertEquals("Mocked response", result.get("llm_response"));
}
```

## Performance Considerations

- Use connection pooling for high-throughput applications
- Consider caching LLM responses for repeated queries
- Use streaming for long-running LLM calls
- Set appropriate timeouts to avoid hanging operations
- Monitor token usage and costs

## Integration with LangGraph Features

### With Human-in-the-Loop

```java
Graph hybridGraph = ChainBuilder.newChain("HumanLLM")
        .addLLMNode("analyze", model, "Analyze: {{input}}", "analysis")
        .addNode(HumanApprovalNode.builder(...)
                .prompt("Approve this analysis?")
                .build())
        .addLLMNode("finalize", model, "Finalize: {{analysis}}", "result")
        .build();
```

### With Persistence

```java
CheckpointManager checkpointManager = new CheckpointManager(store);

// Before execution
String checkpointId = checkpointManager.createCheckpoint(...);

// Execute with LLM nodes
ExecutionResult result = stateGraph.execute(state);

// Save checkpoint after LLM call
if (result.isSuccess()) {
    checkpointManager.updateCheckpoint(checkpointId, result.finalState().data());
}
```

### With Control Flow

```java
Graph controlFlowGraph = ChainBuilder.newChain("ControlFlow")
        .addLLMNode("classify", model, ...)
        .addNode(ConditionalRouterNode.builder(...)
                .route("positive", NodeId.of("handle_positive"))
                .route("negative", NodeId.of("handle_negative"))
                .build())
        .addLLMNode("handle_positive", model, ...)
        .addLLMNode("handle_negative", model, ...)
        .build();
```

## Dependencies

This module requires:

- `langchain4j-core`: Core LangChain4j functionality
- `langgraph-core`: Core LangGraph abstractions
- Provider-specific dependencies (e.g., `langchain4j-open-ai`)

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>
```

## Future Enhancements

- Embedding integration for semantic search
- Memory management for conversational agents
- Multi-modal support (images, audio)
- Advanced function calling with automatic schema generation
- Prompt optimization and caching
- Cost tracking and monitoring
