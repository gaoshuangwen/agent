# MessageGraph - Conversational Workflows

MessageGraph extends the StateGraph execution engine with specialized support for conversational workflows, message routing, history management, and multi-agent conversations.

## Core Concepts

### Message

Immutable message representation with role, content, timestamp, and metadata:

```java
Message message = Message.user("Hello!");
Message response = Message.assistant("Hi there!");
Message system = Message.system("You are a helpful assistant");

// With metadata
Message withMeta = Message.of("user", "Hello", Map.of("source", "api"));

// Update content
Message updated = message.withContent("Updated content");
```

### MessageHistory

Manages conversation history with size limits and filtering:

```java
MessageHistory history = MessageHistory.empty();
history = history.add(Message.user("Hello"));
history = history.add(Message.assistant("Hi!"));

// Get last N messages
List<Message> recent = history.getLastN(10);

// Filter by role
List<Message> userMessages = history.getByRole("user");

// Get latest
Optional<Message> latest = history.getLatest();
Optional<Message> latestUser = history.getLatestByRole("user");

// With size limit
MessageHistory limited = MessageHistory.withMaxSize(100);
```

### MessageContext

Execution context for conversational nodes:

```java
MessageContext context = MessageContext.create(executionContext);

// Access history
MessageHistory history = context.history();

// Add messages
context = context.addMessage(Message.user("Hello"));

// Conversation metadata
context = context.withMetadata("sessionId", "abc123");
String sessionId = context.getMetadata("sessionId", "");
```

## Message Nodes

### MessageProducerNode

Generates messages dynamically:

```java
// Add system context
builder.addProducer("systemContext", "System Context", ctx -> 
    Message.system("You are a helpful assistant")
);
```

### MessageResponderNode

Generates responses based on conversation context:

```java
// Simple responder
builder.addResponder("responder", "Responder", ctx -> {
    String userMessage = ctx.history()
            .getLatestByRole("user")
            .map(Message::content)
            .orElse("");
    
    return "You said: " + userMessage;
});

// Custom role
builder.addResponder("expert", "Expert", "expert", ctx -> 
    "Expert response"
);
```

### MessageRouterNode

Routes conversations based on content:

```java
builder.addRouter("router", "Router", ctx -> {
    String userMessage = ctx.history()
            .getLatestByRole("user")
            .map(Message::content)
            .orElse("")
            .toLowerCase();
    
    if (userMessage.contains("help")) {
        return "help";
    } else if (userMessage.contains("billing")) {
        return "billing";
    } else {
        return "general";
    }
});

// Route edges
builder.addRouteEdge("router", "help", "helpHandler");
builder.addRouteEdge("router", "billing", "billingHandler");
builder.addRouteEdge("router", "general", "generalHandler");
```

### MessageTransformNode

Transforms message history:

```java
// Limit history
builder.addTransformer("limiter", "Limiter", 
    MessageTransformer.limitToLast(10)
);

// Filter by role
builder.addTransformer("filter", "Filter", 
    MessageTransformer.filterByRole("user")
);

// Custom transformer
builder.addTransformer("custom", "Custom", (messages, ctx) -> {
    return messages.stream()
            .filter(m -> !m.content().isEmpty())
            .collect(Collectors.toList());
});

// Chain transformers
MessageTransformer chained = MessageTransformer
        .limitToLast(100)
        .andThen(MessageTransformer.filterByRole("user"));
```

## Building Message Graphs

### Simple Conversation

```java
Graph graph = MessageGraphBuilder.newMessageGraph("SimpleBot")
        .addResponder("bot", "Bot", ctx -> {
            String userName = ctx.history()
                    .getLatestByRole("user")
                    .map(Message::content)
                    .orElse("stranger");
            return "Hello, " + userName + "!";
        })
        .entryPoint("bot")
        .build();

MessageGraph messageGraph = MessageGraph.of(graph);
ExecutionResult result = messageGraph.execute("Alice");

MessageHistory history = messageGraph.extractHistory(result);
// history contains: [user: "Alice", assistant: "Hello, Alice!"]
```

### Multi-Agent Conversation

```java
Graph graph = MessageGraphBuilder.newMessageGraph("MultiAgent")
        // Route to appropriate agent
        .addRouter("dispatcher", "Dispatcher", ctx -> {
            String content = ctx.history()
                    .getLatestByRole("user")
                    .map(Message::content)
                    .orElse("");
            
            if (content.contains("technical")) {
                return "technical";
            } else if (content.contains("sales")) {
                return "sales";
            } else {
                return "general";
            }
        })
        
        // Technical agent
        .addResponder("techAgent", "Technical Agent", "tech-agent", ctx ->
                "Technical support response")
        
        // Sales agent
        .addResponder("salesAgent", "Sales Agent", "sales-agent", ctx ->
                "Sales team response")
        
        // General agent
        .addResponder("generalAgent", "General Agent", ctx ->
                "General support response")
        
        // Routing
        .addRouteEdge("dispatcher", "technical", "techAgent")
        .addRouteEdge("dispatcher", "sales", "salesAgent")
        .addRouteEdge("dispatcher", "general", "generalAgent")
        
        .entryPoint("dispatcher")
        .build();

MessageGraph messageGraph = MessageGraph.of(graph);
```

### Conversation with Context

```java
Graph graph = MessageGraphBuilder.newMessageGraph("ContextAware")
        // Add system context
        .addProducer("systemContext", "System", ctx ->
                Message.system("You are a helpful assistant"))
        
        // Limit history
        .addTransformer("limiter", "Limiter", 
                MessageTransformer.limitToLast(10))
        
        // Generate response
        .addResponder("responder", "Responder", ctx -> {
            int messageCount = ctx.history().size();
            return "I see " + messageCount + " messages in our conversation";
        })
        
        .addEdge("systemContext", "limiter")
        .addEdge("limiter", "responder")
        .entryPoint("systemContext")
        .build();

MessageGraph messageGraph = MessageGraph.of(graph);
```

### Conversation Chain

```java
Graph graph = MessageGraphBuilder.newMessageGraph("Chain")
        // First responder
        .addResponder("analyst", "Analyst", ctx -> {
            String userMsg = ctx.history()
                    .getLatestByRole("user")
                    .map(Message::content)
                    .orElse("");
            return "Analysis: The user said '" + userMsg + "'";
        })
        
        // Second responder
        .addResponder("synthesizer", "Synthesizer", ctx -> {
            String analysis = ctx.history()
                    .getLatestByRole("assistant")
                    .map(Message::content)
                    .orElse("");
            return "Summary: " + analysis;
        })
        
        .addEdge("analyst", "synthesizer")
        .entryPoint("analyst")
        .build();

MessageGraph messageGraph = MessageGraph.of(graph);
```

## Execution

### Execute with String

```java
MessageGraph messageGraph = MessageGraph.of(graph);

// Simple string execution (creates user message automatically)
ExecutionResult result = messageGraph.execute("Hello!");
```

### Execute with Message

```java
// Custom message
Message message = Message.of("user", "Hello", Map.of("source", "api"));
ExecutionResult result = messageGraph.execute(message);
```

### Execute with Multiple Messages

```java
List<Message> messages = List.of(
        Message.system("You are helpful"),
        Message.user("Hello")
);
ExecutionResult result = messageGraph.execute(messages);
```

### Continue Conversation

```java
// Initial conversation
ExecutionResult result1 = messageGraph.execute("Hello");

// Continue with new message
Message followUp = Message.user("How are you?");
ExecutionResult result2 = messageGraph.continueConversation(result1, followUp);

// History is maintained
MessageHistory history = messageGraph.extractHistory(result2);
// Contains all messages from both executions
```

### Async Execution

```java
CompletableFuture<ExecutionResult> future = messageGraph.executeAsync("Hello");

future.thenAccept(result -> {
    MessageHistory history = messageGraph.extractHistory(result);
    System.out.println("Conversation: " + history.size() + " messages");
});
```

## History Management

### Extract History

```java
ExecutionResult result = messageGraph.execute("Hello");

// From result
MessageHistory history = messageGraph.extractHistory(result);

// Access messages
List<Message> messages = history.getMessages();
Optional<Message> latest = history.getLatest();
```

### History Size Limits

```java
// Limit conversation history
MessageGraph messageGraph = MessageGraph.of(graph, config, 100);

// History will automatically truncate to last 100 messages
```

### Filter and Query

```java
MessageHistory history = messageGraph.extractHistory(result);

// Get user messages only
List<Message> userMessages = history.getByRole("user");

// Get last 5 messages
List<Message> recent = history.getLastN(5);

// Get latest by role
Optional<Message> lastUser = history.getLatestByRole("user");
Optional<Message> lastAssistant = history.getLatestByRole("assistant");
```

## Integration with StateGraph

MessageGraph is fully compatible with StateGraph execution:

```java
// Access underlying StateGraph
StateGraph stateGraph = messageGraph.stateGraph();

// Add execution hooks
stateGraph.addExecutionHook(new ExecutionHook() {
    @Override
    public void onNodeStart(ExecutionContext context, NodeId nodeId, State inputState) {
        // Monitor message node execution
    }
});

// Add event listeners
stateGraph.addEventListener(event -> {
    if (event instanceof GraphEvent.NodeExecutionCompleted) {
        // Track message processing
    }
});
```

## Configuration

### Execution Config

```java
ExecutionConfig config = ExecutionConfig.builder()
        .maxConcurrency(4)
        .timeout(Duration.ofMinutes(5))
        .retryPolicy(RetryPolicy.fixedRetry(3, Duration.ofSeconds(1)))
        .enableSnapshots(true)
        .build();

MessageGraph messageGraph = MessageGraph.of(graph, config);
```

### History Limits

```java
// Limit to 50 messages
MessageGraph messageGraph = MessageGraph.of(graph, config, 50);
```

## State Representation

Messages are stored in State as:

```java
State state = State.empty()
        .with("messages", List.of(message1, message2))
        .with("conversationMetadata", Map.of("sessionId", "123"));

// Convert between MessageContext and State
MessageContext context = MessageContext.fromState(state, executionContext);
State newState = context.toState();
```

## Concurrency

MessageGraph supports concurrent execution:

```java
MessageGraph messageGraph = MessageGraph.of(graph);

// Execute multiple conversations concurrently
List<CompletableFuture<ExecutionResult>> futures = new ArrayList<>();
for (int i = 0; i < 10; i++) {
    futures.add(messageGraph.executeAsync("User " + i));
}

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

Each execution maintains its own isolated message history.

## Best Practices

### 1. History Management

Always use history size limits for long-running conversations:

```java
MessageGraph messageGraph = MessageGraph.of(graph, config, 100);
```

### 2. Role Conventions

Use consistent role names:
- `"user"` - User messages
- `"assistant"` - Bot responses
- `"system"` - System instructions
- Custom roles for multi-agent scenarios

### 3. Metadata Usage

Store conversation context in metadata:

```java
context.withMetadata("sessionId", sessionId)
       .withMetadata("userId", userId)
       .withMetadata("channel", "web");
```

### 4. Message Filtering

Use transformers to prepare context:

```java
// Only pass relevant messages to expensive nodes
builder.addTransformer("filter", "Filter", 
    MessageTransformer.limitToLast(10)
            .andThen(MessageTransformer.filterByRole("user"))
);
```

### 5. Error Handling

Handle conversation errors gracefully:

```java
ExecutionResult result = messageGraph.execute("User input");

if (result.isFailed()) {
    // Fallback response
    Message error = Message.assistant("Sorry, I encountered an error");
    // Handle error
}
```

### 6. Resource Management

Always shutdown when done:

```java
try {
    ExecutionResult result = messageGraph.execute("Hello");
    // Process result
} finally {
    messageGraph.shutdown();
}
```

## Examples

### Customer Support Bot

```java
Graph supportBot = MessageGraphBuilder.newMessageGraph("Support")
        .addRouter("classifier", "Classifier", ctx -> {
            String msg = ctx.history().getLatestByRole("user")
                    .map(Message::content).orElse("");
            if (msg.contains("refund")) return "refund";
            if (msg.contains("tracking")) return "tracking";
            return "general";
        })
        .addResponder("refundHandler", "Refund", ctx ->
                "I'll help you with your refund request")
        .addResponder("trackingHandler", "Tracking", ctx ->
                "Let me check your order status")
        .addResponder("generalHandler", "General", ctx ->
                "How can I help you today?")
        .addRouteEdge("classifier", "refund", "refundHandler")
        .addRouteEdge("classifier", "tracking", "trackingHandler")
        .addRouteEdge("classifier", "general", "generalHandler")
        .entryPoint("classifier")
        .build();
```

### Multi-Turn Conversation

```java
MessageGraph messageGraph = MessageGraph.of(graph);

// Turn 1
ExecutionResult result1 = messageGraph.execute("Hello");

// Turn 2
result1 = messageGraph.continueConversation(result1, 
        Message.user("What's the weather?"));

// Turn 3
result1 = messageGraph.continueConversation(result1,
        Message.user("Thanks!"));

// Full conversation history maintained
MessageHistory fullHistory = messageGraph.extractHistory(result1);
```

## Testing

Comprehensive test coverage:
- Message creation and manipulation
- History management and filtering
- Simple and complex conversation flows
- Message routing and transformations
- Concurrent execution
- State consistency
- Integration with StateGraph

Run tests:
```bash
mvn test -pl langgraph-core -Dtest="*message*"
```
