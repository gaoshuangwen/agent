# Control Flow Constructs

Advanced control flow constructs for LangGraph including conditional routing, loops, recursive subgraphs, and branch merging with comprehensive protection against infinite loops and excessive recursion.

## Core Components

### GuardCondition

Reusable predicates for controlling execution flow:

```java
// Built-in conditions
GuardCondition always = GuardCondition.always();
GuardCondition never = GuardCondition.never();
GuardCondition hasKey = GuardCondition.hasKey("myKey");
GuardCondition equals = GuardCondition.equals("status", "ready");
GuardCondition gt = GuardCondition.greaterThan("count", 5);
GuardCondition lt = GuardCondition.lessThan("count", 10);

// Combine conditions
GuardCondition combined = GuardCondition.hasKey("count")
        .and(GuardCondition.greaterThan("count", 0));

GuardCondition either = GuardCondition.equals("status", "ready")
        .or(GuardCondition.equals("status", "active"));

GuardCondition negated = GuardCondition.hasKey("error").negate();

// Custom condition
GuardCondition custom = GuardCondition.of((state, ctx) -> 
    state.get("value", 0) % 2 == 0
);
```

## Conditional Routing

### ConditionalRouterNode

Route execution to different paths based on state conditions:

```java
ConditionalRouterNode router = ConditionalRouterNode.builder(
        NodeId.of("router"), "Priority Router")
        .route("highPriority", GuardCondition.greaterThan("priority", 8))
        .route("mediumPriority", GuardCondition.greaterThan("priority", 4))
        .route("lowPriority", GuardCondition.greaterThan("priority", 0))
        .defaultRoute("fallback")
        .build();

Graph graph = GraphBuilder.newGraph("ConditionalFlow")
        .addNode(router)
        .addFunctionalNode(NodeId.of("high"), "High", 
                (state, ctx) -> state.with("handled", "high"))
        .addFunctionalNode(NodeId.of("medium"), "Medium", 
                (state, ctx) -> state.with("handled", "medium"))
        .addFunctionalNode(NodeId.of("low"), "Low", 
                (state, ctx) -> state.with("handled", "low"))
        .addFunctionalNode(NodeId.of("fallback"), "Fallback", 
                (state, ctx) -> state.with("handled", "fallback"))
        
        // Connect routes
        .addConditionalEdge(NodeId.of("router"), NodeId.of("high"),
                (state, ctx) -> router.getSelectedRoute(state).equals("highPriority"))
        .addConditionalEdge(NodeId.of("router"), NodeId.of("medium"),
                (state, ctx) -> router.getSelectedRoute(state).equals("mediumPriority"))
        .addConditionalEdge(NodeId.of("router"), NodeId.of("low"),
                (state, ctx) -> router.getSelectedRoute(state).equals("lowPriority"))
        .addConditionalEdge(NodeId.of("router"), NodeId.of("fallback"),
                (state, ctx) -> router.getSelectedRoute(state).equals("fallback"))
        
        .entryPoint(NodeId.of("router"))
        .build();
```

### Route Selection

Routes are evaluated in order until a condition matches:

```java
ConditionalRouterNode router = ConditionalRouterNode.builder(nodeId, name)
        .route("route1", condition1)  // Checked first
        .route("route2", condition2)  // Checked second
        .route("route3", condition3)  // Checked third
        .defaultRoute("default")      // Fallback if no match
        .build();
```

## Loop Constructs

### LoopNode

Execute a loop with termination conditions and iteration limits:

```java
LoopNode counterLoop = LoopNode.builder(NodeId.of("loop"), "Counter")
        .condition(GuardCondition.lessThan("count", 10))
        .body((state, ctx) -> {
            int count = state.get("count", 0);
            return state.with("count", count + 1);
        })
        .maxIterations(100)  // Protection against infinite loops
        .build();

Graph graph = GraphBuilder.newGraph("LoopExample")
        .addNode(counterLoop)
        .entryPoint(NodeId.of("loop"))
        .build();

StateGraph stateGraph = StateGraph.of(graph);
ExecutionResult result = stateGraph.execute(State.of(Map.of("count", 0)));

// Result state contains:
// - count: 10
// - __loop_iteration__: 10
// - __loop_completed__: true
```

### Loop Protection

Infinite loop protection with configurable limits:

```java
LoopNode protectedLoop = LoopNode.builder(nodeId, name)
        .condition(GuardCondition.always())  // Would loop forever
        .body((state, ctx) -> state.with("tick", true))
        .maxIterations(100)  // Throws after 100 iterations
        .build();

// Execution will fail with LoopDetector.InfiniteLoopException
```

### Accumulator Pattern

```java
LoopNode accumulator = LoopNode.builder(NodeId.of("sum"), "Accumulator")
        .condition(GuardCondition.lessThan("sum", 1000))
        .body((state, ctx) -> {
            int sum = state.get("sum", 0);
            int increment = state.get("increment", 1);
            return state.with("sum", sum + increment);
        })
        .maxIterations(2000)
        .build();
```

## Recursive Subgraphs

### SubgraphNode

Execute a subgraph recursively with depth limits:

```java
// Define a subgraph
Graph processingGraph = GraphBuilder.newGraph("Processing")
        .addFunctionalNode(NodeId.of("process"), "Process", 
                (state, ctx) -> state.with("processed", true))
        .entryPoint(NodeId.of("process"))
        .build();

// Create subgraph node with recursion protection
SubgraphNode subgraphNode = SubgraphNode.builder(
        NodeId.of("subgraph"), "Processor")
        .subgraph(processingGraph)
        .maxDepth(10)  // Maximum recursion depth
        .config(ExecutionConfig.defaults())
        .build();

Graph mainGraph = GraphBuilder.newGraph("Main")
        .addNode(subgraphNode)
        .entryPoint(NodeId.of("subgraph"))
        .build();
```

### Nested Subgraphs

```java
// Inner subgraph
Graph innerGraph = GraphBuilder.newGraph("Inner")
        .addFunctionalNode(NodeId.of("inner"), "Inner", 
                (state, ctx) -> state.with("level", "inner"))
        .entryPoint(NodeId.of("inner"))
        .build();

SubgraphNode innerNode = SubgraphNode.builder(
        NodeId.of("innerSub"), "InnerSub")
        .subgraph(innerGraph)
        .maxDepth(5)
        .build();

// Outer subgraph containing inner subgraph
Graph outerGraph = GraphBuilder.newGraph("Outer")
        .addNode(innerNode)
        .addFunctionalNode(NodeId.of("outer"), "Outer", 
                (state, ctx) -> state.with("level", "outer"))
        .addDirectEdge(NodeId.of("innerSub"), NodeId.of("outer"))
        .entryPoint(NodeId.of("innerSub"))
        .build();

SubgraphNode outerNode = SubgraphNode.builder(
        NodeId.of("outerSub"), "OuterSub")
        .subgraph(outerGraph)
        .maxDepth(5)
        .build();
```

### Recursion Depth Tracking

The `__recursion_depth__` key tracks recursion depth automatically:

```java
SubgraphNode node = SubgraphNode.builder(nodeId, name)
        .subgraph(graph)
        .maxDepth(3)
        .build();

// Throws RecursionDepthExceededException if depth > 3
```

## Branch Merging

### BranchMergeNode

Merge state from multiple parallel branches:

```java
BranchMergeNode merge = BranchMergeNode.builder(
        NodeId.of("merge"), "Merge")
        .merger(StateMerger.combine())  // Combine all branch states
        .build();

// Branch states are collected in __branch_states__ key
List<State> branchStates = List.of(
        State.of(Map.of("a", 1, "b", 2)),
        State.of(Map.of("c", 3, "d", 4))
);

State inputState = State.of(Map.of("__branch_states__", branchStates));
// After merge: {a=1, b=2, c=3, d=4}
```

### Merge Strategies

#### Combine (Default)
Combines all keys from all branches:

```java
BranchMergeNode merge = BranchMergeNode.builder(nodeId, name)
        .merger(StateMerger.combine())
        .build();

// Branch 1: {a=1, b=2}
// Branch 2: {c=3, d=4}
// Result:   {a=1, b=2, c=3, d=4}
```

#### First
Uses state from first branch:

```java
BranchMergeNode merge = BranchMergeNode.builder(nodeId, name)
        .merger(StateMerger.first())
        .build();

// Branch 1: {result="first"}
// Branch 2: {result="second"}
// Result:   {result="first"}
```

#### Last
Uses state from last branch:

```java
BranchMergeNode merge = BranchMergeNode.builder(nodeId, name)
        .merger(StateMerger.last())
        .build();

// Branch 1: {result="first"}
// Branch 2: {result="second"}
// Result:   {result="second"}
```

#### Priority Merge
Later branches override earlier branches for duplicate keys:

```java
BranchMergeNode merge = BranchMergeNode.builder(nodeId, name)
        .merger(StateMerger.priorityMerge())
        .build();

// Branch 1: {a=1, b=2}
// Branch 2: {a=10, c=3}
// Result:   {a=1, b=2, c=3}  // First branch wins for 'a'
```

#### Custom Merge
Define custom merge logic:

```java
BranchMergeNode merge = BranchMergeNode.builder(nodeId, name)
        .merger(states -> {
            int sum = 0;
            for (State state : states) {
                sum += state.get("value", 0);
            }
            return State.of(Map.of("total", sum));
        })
        .build();

// Branch 1: {value=10}
// Branch 2: {value=20}
// Branch 3: {value=30}
// Result:   {total=60}
```

## Loop Detection

### LoopDetector

Tracks node visits and prevents infinite loops:

```java
LoopDetector detector = new LoopDetector(100);  // Max 100 visits per node

NodeId nodeId = NodeId.of("repeating");

detector.recordVisit(nodeId);  // Visit 1
detector.recordVisit(nodeId);  // Visit 2
// ... up to 100 times

detector.recordVisit(nodeId);  // Throws InfiniteLoopException
```

### Per-Node Tracking

```java
LoopDetector detector = new LoopDetector(10);

NodeId node1 = NodeId.of("node1");
NodeId node2 = NodeId.of("node2");

detector.recordVisit(node1);  // node1: 1 visit
detector.recordVisit(node2);  // node2: 1 visit
detector.recordVisit(node1);  // node1: 2 visits

assertEquals(2, detector.getVisitCount(node1));
assertEquals(1, detector.getVisitCount(node2));
```

### Reset Detection

```java
LoopDetector detector = new LoopDetector(10);

// Record some visits
detector.recordVisit(nodeId);
detector.recordVisit(nodeId);

// Reset for new execution
detector.reset();

assertEquals(0, detector.getVisitCount(nodeId));
```

## Complete Examples

### Decision Tree

```java
Graph decisionTree = GraphBuilder.newGraph("DecisionTree")
        // Input validation
        .addFunctionalNode(NodeId.of("validate"), "Validate", 
                (state, ctx) -> {
                    int value = state.get("value", 0);
                    return state.with("valid", value >= 0);
                })
        
        // Router
        .addNode(ConditionalRouterNode.builder(NodeId.of("router"), "Router")
                .route("positive", GuardCondition.greaterThan("value", 0))
                .route("zero", GuardCondition.equals("value", 0))
                .route("invalid", GuardCondition.equals("valid", false))
                .build())
        
        // Handlers
        .addFunctionalNode(NodeId.of("positive"), "Positive", 
                (state, ctx) -> state.with("result", "positive"))
        .addFunctionalNode(NodeId.of("zero"), "Zero", 
                (state, ctx) -> state.with("result", "zero"))
        .addFunctionalNode(NodeId.of("invalid"), "Invalid", 
                (state, ctx) -> state.with("result", "invalid"))
        
        .addDirectEdge(NodeId.of("validate"), NodeId.of("router"))
        .addConditionalEdge(NodeId.of("router"), NodeId.of("positive"),
                (state, ctx) -> state.get("__route__", "").equals("positive"))
        .addConditionalEdge(NodeId.of("router"), NodeId.of("zero"),
                (state, ctx) -> state.get("__route__", "").equals("zero"))
        .addConditionalEdge(NodeId.of("router"), NodeId.of("invalid"),
                (state, ctx) -> state.get("__route__", "").equals("invalid"))
        
        .entryPoint(NodeId.of("validate"))
        .build();
```

### Iterative Processing

```java
Graph iterativeProcessor = GraphBuilder.newGraph("Processor")
        // Initialize
        .addFunctionalNode(NodeId.of("init"), "Initialize", 
                (state, ctx) -> state.with("items", List.of(1, 2, 3, 4, 5))
                        .with("processed", new ArrayList<>())
                        .with("index", 0))
        
        // Loop to process each item
        .addNode(LoopNode.builder(NodeId.of("loop"), "ProcessLoop")
                .condition(GuardCondition.of((state, ctx) -> {
                    int index = state.get("index", 0);
                    List<?> items = state.get("items", List.of());
                    return index < items.size();
                }))
                .body((state, ctx) -> {
                    int index = state.get("index", 0);
                    List<Integer> items = state.get("items", List.of());
                    List<Integer> processed = state.get("processed", new ArrayList<>());
                    
                    // Process item
                    int item = items.get(index);
                    processed = new ArrayList<>(processed);
                    processed.add(item * 2);
                    
                    return state.with("processed", processed)
                            .with("index", index + 1);
                })
                .maxIterations(100)
                .build())
        
        .addDirectEdge(NodeId.of("init"), NodeId.of("loop"))
        .entryPoint(NodeId.of("init"))
        .build();
```

### Recursive Tree Traversal

```java
// Subgraph for processing a node
Graph nodeProcessor = GraphBuilder.newGraph("NodeProcessor")
        .addFunctionalNode(NodeId.of("process"), "Process", 
                (state, ctx) -> {
                    // Process current node
                    return state.with("visited", true);
                })
        .entryPoint(NodeId.of("process"))
        .build();

// Recursive traversal
SubgraphNode recursiveNode = SubgraphNode.builder(
        NodeId.of("recursive"), "Recursive")
        .subgraph(nodeProcessor)
        .maxDepth(20)  // Tree depth limit
        .build();

Graph treeTraversal = GraphBuilder.newGraph("TreeTraversal")
        .addNode(recursiveNode)
        // ... add logic to traverse children
        .entryPoint(NodeId.of("recursive"))
        .build();
```

## Error Handling

### Infinite Loop Protection

```java
try {
    ExecutionResult result = stateGraph.execute(initialState);
} catch (LoopDetector.InfiniteLoopException e) {
    System.err.println("Infinite loop detected: " + e.getMessage());
    // Message contains node ID, iteration count, and limit
}
```

### Recursion Depth Protection

```java
try {
    ExecutionResult result = stateGraph.execute(initialState);
} catch (SubgraphNode.RecursionDepthExceededException e) {
    System.err.println("Recursion depth exceeded: " + e.getMessage());
    // Message contains node ID and depth limit
}
```

### Subgraph Execution Failures

```java
try {
    ExecutionResult result = stateGraph.execute(initialState);
} catch (SubgraphNode.SubgraphExecutionException e) {
    System.err.println("Subgraph failed: " + e.getMessage());
    Throwable cause = e.getCause();
    // Handle underlying error
}
```

## Best Practices

### 1. Set Appropriate Limits

Always configure limits based on your use case:

```java
// Loops
LoopNode loop = LoopNode.builder(nodeId, name)
        .condition(condition)
        .body(body)
        .maxIterations(1000)  // Reasonable limit
        .build();

// Recursion
SubgraphNode subgraph = SubgraphNode.builder(nodeId, name)
        .subgraph(graph)
        .maxDepth(10)  // Reasonable depth
        .build();
```

### 2. Use Guard Conditions Effectively

Combine simple conditions for complex logic:

```java
GuardCondition readyToProcess = GuardCondition
        .hasKey("data")
        .and(GuardCondition.hasKey("config"))
        .and(GuardCondition.equals("status", "ready"));
```

### 3. Provide Default Routes

Always provide a default route for robustness:

```java
ConditionalRouterNode router = ConditionalRouterNode.builder(nodeId, name)
        .route("special", specialCondition)
        .route("normal", normalCondition)
        .defaultRoute("fallback")  // Handles unexpected cases
        .build();
```

### 4. Clean Up Resources

Shutdown subgraph nodes when done:

```java
SubgraphNode subgraph = SubgraphNode.builder(nodeId, name)
        .subgraph(graph)
        .build();

try {
    // Use subgraph
} finally {
    subgraph.shutdown();
}
```

### 5. Monitor Loop Iterations

Track loop progress using metadata:

```java
LoopNode loop = LoopNode.builder(nodeId, name)
        .condition(condition)
        .body((state, ctx) -> {
            int iteration = state.get("__loop_iteration__", 0);
            // Log or monitor iteration count
            return processedState;
        })
        .build();
```

## Testing

Comprehensive test coverage for:
- Guard condition combinations
- Conditional routing paths
- Loop termination
- Infinite loop protection
- Recursion depth limits
- Subgraph execution
- Branch merging strategies
- State consistency

Run tests:
```bash
mvn test -pl langgraph-core -Dtest="*control*"
```

## Performance Considerations

- Loop limits prevent runaway executions
- Recursion depth limits prevent stack overflow
- Branch merging can be optimized with custom strategies
- Subgraph nodes create isolated execution contexts

## Thread Safety

All control flow constructs are thread-safe:
- GuardConditions can be reused across threads
- LoopDetector maintains thread-safe visit counts
- StateMerger implementations are stateless
- SubgraphNodes use isolated StateGraph instances
