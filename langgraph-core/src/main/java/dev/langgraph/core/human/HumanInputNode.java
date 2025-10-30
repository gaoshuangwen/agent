package dev.langgraph.core.human;

import dev.langgraph.core.*;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class HumanInputNode extends AbstractNode {
    
    private final String prompt;
    private final Duration timeout;
    private final TimeoutStrategy timeoutStrategy;
    private final HumanTaskManager taskManager;
    private final String outputKey;

    public HumanInputNode(NodeId id, String name, Map<String, Object> metadata,
                         String prompt, String outputKey, Duration timeout, 
                         TimeoutStrategy timeoutStrategy, HumanTaskManager taskManager) {
        super(id, name, metadata);
        this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
        this.outputKey = Objects.requireNonNull(outputKey, "Output key cannot be null");
        this.timeout = timeout;
        this.timeoutStrategy = timeoutStrategy != null ? timeoutStrategy : TimeoutStrategy.fail();
        this.taskManager = Objects.requireNonNull(taskManager, "TaskManager cannot be null");
    }

    @Override
    protected State doExecute(State inputState, ExecutionContext context) throws Exception {
        HumanTask existingTask = findPendingTask(context.executionId());
        
        if (existingTask != null) {
            if (existingTask.isExpired()) {
                return timeoutStrategy.onTimeout(existingTask, inputState, context);
            }
            
            if (existingTask.isCompleted()) {
                return processInput(existingTask, inputState);
            }
            
            throw new HumanTaskPendingException(existingTask);
        }

        HumanTask task = taskManager.createTask(
                context.executionId(),
                id().value(),
                HumanTaskType.INPUT,
                prompt,
                inputState.data(),
                timeout
        );

        throw new HumanTaskPendingException(task);
    }

    private HumanTask findPendingTask(String executionId) {
        return taskManager.getStore().listByExecutionId(executionId).stream()
                .filter(t -> t.nodeId().equals(id().value()))
                .findFirst()
                .orElse(null);
    }

    private State processInput(HumanTask task, State inputState) {
        Map<String, Object> response = task.response();
        Object inputValue = response.get("input");
        
        return inputState
                .with(outputKey, inputValue)
                .with("__human_task_id__", task.taskId())
                .with("__human_response__", response);
    }

    public static Builder builder(NodeId id, String name, HumanTaskManager taskManager) {
        return new Builder(id, name, taskManager);
    }

    public static final class Builder {
        private final NodeId id;
        private final String name;
        private final HumanTaskManager taskManager;
        private final Map<String, Object> metadata = new java.util.HashMap<>();
        private String prompt;
        private String outputKey = "humanInput";
        private Duration timeout;
        private TimeoutStrategy timeoutStrategy;

        private Builder(NodeId id, String name, HumanTaskManager taskManager) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
            this.taskManager = Objects.requireNonNull(taskManager);
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder timeoutStrategy(TimeoutStrategy strategy) {
            this.timeoutStrategy = strategy;
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public HumanInputNode build() {
            if (prompt == null) {
                prompt = "Input required";
            }
            return new HumanInputNode(id, name, metadata, prompt, outputKey, timeout, timeoutStrategy, taskManager);
        }
    }
}
