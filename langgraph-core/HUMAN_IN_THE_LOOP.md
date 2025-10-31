# Human-in-the-Loop Support

LangGraph provides comprehensive human-in-the-loop (HIL) capabilities, allowing workflows to pause for human approval or input with timeout handling and resumption.

## Features

- **Interruptible Nodes**: Pause execution for human feedback
- **Human Task Management**: Track and manage pending human tasks
- **Timeout Handling**: Configurable timeout with escalation strategies
- **Resumable Execution**: Resume from where you left off after human response
- **Event Publishing**: Subscribe to human task lifecycle events
- **Persistence Integration**: Store pending tasks for long-running workflows

## Core Components

### HumanTask

Represents a pending human interaction:

```java
HumanTask task = HumanTask.builder()
        .executionId("exec-123")
        .nodeId("approval-node")
        .type(HumanTaskType.APPROVAL)
        .prompt("Approve this deletion?")
        .context(Map.of("resource", "user-123"))
        .timeout(Duration.ofMinutes(30))
        .build();
```

**Task Types**:
- `APPROVAL`: Yes/no decision
- `INPUT`: Free-form input collection
- `REVIEW`: Document/data review
- `DECISION`: Multi-choice decision

**Task Status**:
- `PENDING`: Awaiting human response
- `COMPLETED`: Human has responded
- `EXPIRED`: Timeout exceeded
- `CANCELLED`: Task cancelled
- `ESCALATED`: Escalated to higher authority

### HumanTaskManager

Coordinates human task lifecycle:

```java
HumanTaskStore store = new InMemoryHumanTaskStore();
HumanTaskManager manager = new HumanTaskManager(store, eventPublisher);

// Create task
HumanTask task = manager.createTask(
        executionId,
        nodeId,
        HumanTaskType.APPROVAL,
        "Approve deployment?",
        contextData,
        Duration.ofHours(2)
);

// Complete task
manager.completeTask(task.taskId(), Map.of(
        "approved", true,
        "comments", "Looks good"
));

// Cancel task
manager.cancelTask(task.taskId());

// Escalate task
manager.escalateTask(task.taskId());
```

## Interruptible Nodes

### HumanApprovalNode

Pauses execution for approval:

```java
HumanApprovalNode approvalNode = HumanApprovalNode.builder(
                NodeId.of("approval"), "Approval", taskManager)
        .prompt("Approve this action?")
        .timeout(Duration.ofHours(24))
        .timeoutStrategy(TimeoutStrategy.escalate("manager-approval"))
        .metadata("criticality", "high")
        .build();
```

**Output State Keys**:
- `__human_approved__`: Boolean approval result
- `__human_task_id__`: Task identifier
- `__human_comments__`: Optional comments from approver

### HumanInputNode

Collects input from human:

```java
HumanInputNode inputNode = HumanInputNode.builder(
                NodeId.of("input"), "Input", taskManager)
        .prompt("Enter project name")
        .outputKey("projectName")
        .timeout(Duration.ofMinutes(10))
        .timeoutStrategy(TimeoutStrategy.continueWithDefault(
                Map.of("input", "Unnamed Project")
        ))
        .build();
```

**Output**:
- Custom key (specified by `outputKey`)
- `__human_task_id__`: Task identifier
- `__human_response__`: Full response object

## Timeout Strategies

### Fail Strategy

Throw exception on timeout (default):

```java
TimeoutStrategy strategy = TimeoutStrategy.fail();
```

### Continue with Default

Use default value on timeout:

```java
TimeoutStrategy strategy = TimeoutStrategy.continueWithDefault(
        Map.of("approved", false, "reason", "timeout")
);
```

### Escalate

Route to escalation path:

```java
TimeoutStrategy strategy = TimeoutStrategy.escalate("escalation-node");
```

**Escalation State Keys**:
- `__escalate_to__`: Node ID to escalate to
- `__escalated_task__`: Original task ID
- `__human_task_timed_out__`: True

### Custom Strategy

Implement custom logic:

```java
TimeoutStrategy strategy = TimeoutStrategy.custom((task, state, ctx) -> {
    // Custom timeout handling
    return state.with("timeout_handled", true);
});
```

## Execution Flow

### Basic Flow

```java
// 1. Create graph with human approval node
Graph graph = GraphBuilder.newGraph("ApprovalWorkflow")
        .addFunctionalNode(NodeId.of("start"), "Start",
                (state, ctx) -> state.with("action", "delete"))
        .addNode(approvalNode)
        .addFunctionalNode(NodeId.of("execute"), "Execute",
                (state, ctx) -> {
                    boolean approved = state.get("__human_approved__", false);
                    if (approved) {
                        // Perform action
                    }
                    return state.with("executed", approved);
                })
        .addDirectEdge(NodeId.of("start"), NodeId.of("approval"))
        .addDirectEdge(NodeId.of("approval"), NodeId.of("execute"))
        .entryPoint(NodeId.of("start"))
        .build();

// 2. First execution - will pause
try {
    StateGraph stateGraph = StateGraph.of(graph);
    stateGraph.execute(State.empty());
} catch (Exception e) {
    // Execution paused for human input
}

// 3. Check pending tasks
List<HumanTask> pending = taskManager.getPendingTasks();
HumanTask task = pending.get(0);

// 4. Complete task (human provides response)
taskManager.completeTask(task.taskId(), Map.of(
        "approved", true,
        "comments", "Approved by manager"
));

// 5. Resume execution
StateGraph stateGraph = StateGraph.of(graph);
ExecutionResult result = stateGraph.execute(State.empty());
// Execution completes with approval
```

### Multi-Step Approval

```java
Graph graph = GraphBuilder.newGraph("MultiStepApproval")
        .addNode(techApproval)    // First approval
        .addNode(managerApproval)  // Second approval
        .addNode(financeApproval)  // Third approval
        .addFunctionalNode(NodeId.of("execute"), "Execute",
                (state, ctx) -> state.with("executed", true))
        .addDirectEdge(NodeId.of("tech"), NodeId.of("manager"))
        .addDirectEdge(NodeId.of("manager"), NodeId.of("finance"))
        .addDirectEdge(NodeId.of("finance"), NodeId.of("execute"))
        .entryPoint(NodeId.of("tech"))
        .build();

// Each approval pause's execution until completed
// Workflow progresses through each approval stage
```

## Event Integration

Subscribe to human task events:

```java
EventPublisher publisher = new CompositeEventPublisher();
publisher.addListener(event -> {
    if (event instanceof GraphEvent.HumanTaskCreated e) {
        // Notify user about pending task
        notifyUser(e.taskId(), e.nodeId());
    }
    else if (event instanceof GraphEvent.HumanTaskCompleted e) {
        // Log completion
        logger.info("Task completed: " + e.taskId());
    }
    else if (event instanceof GraphEvent.HumanTaskExpired e) {
        // Handle expiration
        escalateTask(e.taskId());
    }
});

HumanTaskManager manager = new HumanTaskManager(store, publisher);
```

**Human Task Events**:
- `HumanTaskCreated`: New task created
- `HumanTaskCompleted`: Task completed by human
- `HumanTaskExpired`: Task expired
- `HumanTaskCancelled`: Task cancelled
- `HumanTaskEscalated`: Task escalated

## Storage Backends

### In-Memory Store

For testing and short-lived processes:

```java
HumanTaskStore store = new InMemoryHumanTaskStore();
```

**Characteristics**:
- Thread-safe
- Fast operations
- Lost on process restart
- Suitable for development/testing

### Persistence Integration

For production use, integrate with checkpoint storage:

```java
// Use persistence module for durable storage
CheckpointManager checkpointManager = new CheckpointManager(
        new FileBasedCheckpointStore(storagePath)
);

// Store human tasks alongside checkpoints
HumanTask task = manager.createTask(...);
checkpointManager.createCheckpoint(
        graphId,
        executionId,
        state.data(),
        currentNodeId,
        Map.of("pendingHumanTask", task.taskId())
);
```

## REST API Integration

Example REST endpoints for human task management:

```java
@RestController
@RequestMapping("/api/human-tasks")
public class HumanTaskController {
    
    private final HumanTaskManager taskManager;
    
    @GetMapping("/pending")
    public List<HumanTask> getPendingTasks() {
        return taskManager.getPendingTasks();
    }
    
    @GetMapping("/{taskId}")
    public HumanTask getTask(@PathVariable("taskId") String taskId) {
        return taskManager.getTask(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
    }
    
    @PostMapping("/{taskId}/complete")
    public void completeTask(@PathVariable("taskId") String taskId, 
                            @RequestBody Map<String, Object> response) {
        taskManager.completeTask(taskId, response);
    }
    
    @PostMapping("/{taskId}/cancel")
    public void cancelTask(@PathVariable String taskId) {
        taskManager.cancelTask(taskId);
    }
}
```

## WebSocket Notifications

Real-time task notifications:

```java
@Component
public class HumanTaskNotifier implements EventListener {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @Override
    public void onEvent(GraphEvent event) {
        if (event instanceof GraphEvent.HumanTaskCreated e) {
            messagingTemplate.convertAndSend(
                    "/topic/human-tasks",
                    new TaskNotification(e.taskId(), e.taskType(), e.metadata())
            );
        }
    }
}
```

## Best Practices

### 1. Set Appropriate Timeouts

```java
// Short timeout for quick approvals
.timeout(Duration.ofMinutes(5))

// Long timeout for review tasks
.timeout(Duration.ofHours(24))

// Very long for multi-day processes
.timeout(Duration.ofDays(3))
```

### 2. Provide Clear Prompts

```java
.prompt("Approve deployment of version 2.5 to production? " +
        "This will affect 10,000 users.")
.context(Map.of(
        "version", "2.5",
        "environment", "production",
        "impactedUsers", 10000
))
```

### 3. Handle Timeouts Gracefully

```java
TimeoutStrategy strategy = TimeoutStrategy.custom((task, state, ctx) -> {
    // Log timeout
    logger.warn("Task timed out: " + task.taskId());
    
    // Escalate based on criticality
    String criticality = (String) task.context().get("criticality");
    if ("high".equals(criticality)) {
        return state.with("__escalate_to__", "urgent-review");
    } else {
        return state.with("auto_approved", false);
    }
});
```

### 4. Clean Up Completed Tasks

```java
// After execution completes
executionResult.ifSuccess(result -> {
    taskManager.deleteTasksForExecution(executionId);
});
```

### 5. Monitor Pending Tasks

```java
// Periodic check for expired tasks
@Scheduled(fixedRate = 60000) // Every minute
public void checkExpiredTasks() {
    List<HumanTask> expired = taskManager.getExpiredTasks();
    for (HumanTask task : expired) {
        processExpiredTask(task);
    }
}
```

### 6. Provide Task Context

```java
.context(Map.of(
        "requestId", "REQ-123",
        "requester", "john.doe@example.com",
        "timestamp", Instant.now(),
        "details", detailsObject
))
```

## Testing

### Unit Testing

```java
@Test
void shouldPauseForApproval() {
    HumanApprovalNode node = HumanApprovalNode.builder(
                    NodeId.of("approval"), "Test", taskManager)
            .prompt("Approve?")
            .build();

    assertThrows(HumanTaskPendingException.class, () -> {
        node.execute(State.empty(), context, EventPublisher.noOp());
    });

    assertEquals(1, taskManager.getPendingTasks().size());
}

@Test
void shouldResumeAfterApproval() throws Exception {
    // Create task
    node.execute(State.empty(), context, EventPublisher.noOp());
    
    // Complete task
    HumanTask task = taskManager.getPendingTasks().get(0);
    taskManager.completeTask(task.taskId(), Map.of("approved", true));
    
    // Resume
    State result = node.execute(State.empty(), context, EventPublisher.noOp());
    assertTrue(result.get("__human_approved__", false));
}
```

### Integration Testing

```java
@Test
void shouldHandleEndToEndFlow() {
    // Setup graph with human nodes
    Graph graph = createApprovalWorkflow();
    StateGraph stateGraph = StateGraph.of(graph);
    
    // Start execution
    stateGraph.execute(initialState);
    
    // Verify task created
    List<HumanTask> tasks = taskManager.getPendingTasks();
    assertEquals(1, tasks.size());
    
    // Simulate human response
    taskManager.completeTask(tasks.get(0).taskId(), 
            Map.of("approved", true));
    
    // Resume and verify completion
    ExecutionResult result = stateGraph.execute(initialState);
    assertTrue(result.isSuccess());
}
```

## Examples

See test files for complete examples:
- `HumanTaskTest`: Task management
- `HumanTaskManagerTest`: Manager operations
- `HumanInTheLoopIntegrationTest`: End-to-end HIL flows

## Error Handling

```java
try {
    taskManager.completeTask(taskId, response);
} catch (IllegalArgumentException e) {
    // Task not found
} catch (IllegalStateException e) {
    // Task not in PENDING status
} catch (HumanTaskTimeoutException e) {
    // Task has expired
}
```

## Thread Safety

- `HumanTaskManager`: Thread-safe when using thread-safe stores
- `InMemoryHumanTaskStore`: Thread-safe with `ConcurrentHashMap`
- `HumanTask`: Immutable and thread-safe
- Event publishing: Thread-safe

## Performance Considerations

- Use appropriate timeouts to avoid resource leaks
- Clean up completed tasks regularly
- Consider pagination for large task lists
- Use indexed storage for production (database)
- Cache frequently accessed tasks

## Future Enhancements

- Database-backed task store
- Task priority and SLA tracking
- Delegation and reassignment
- Approval chains and workflows
- Integration with external task systems
- Mobile notifications
- Task analytics and reporting
