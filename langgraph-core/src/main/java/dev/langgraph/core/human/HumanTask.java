package dev.langgraph.core.human;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class HumanTask {
    
    private final String taskId;
    private final String executionId;
    private final String nodeId;
    private final HumanTaskType type;
    private final String prompt;
    private final Map<String, Object> context;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final HumanTaskStatus status;
    private final Map<String, Object> response;
    private final Instant completedAt;

    @JsonCreator
    private HumanTask(@JsonProperty("taskId") String taskId,
                     @JsonProperty("executionId") String executionId,
                     @JsonProperty("nodeId") String nodeId,
                     @JsonProperty("type") HumanTaskType type,
                     @JsonProperty("prompt") String prompt,
                     @JsonProperty("context") Map<String, Object> context,
                     @JsonProperty("createdAt") Instant createdAt,
                     @JsonProperty("expiresAt") Instant expiresAt,
                     @JsonProperty("status") HumanTaskStatus status,
                     @JsonProperty("response") Map<String, Object> response,
                     @JsonProperty("completedAt") Instant completedAt) {
        this.taskId = Objects.requireNonNull(taskId, "Task ID cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "Execution ID cannot be null");
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.type = Objects.requireNonNull(type, "Task type cannot be null");
        this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
        this.context = context == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(context));
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        this.expiresAt = expiresAt;
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.response = response == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(response));
        this.completedAt = completedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("taskId")
    public String taskId() {
        return taskId;
    }

    @JsonProperty("executionId")
    public String executionId() {
        return executionId;
    }

    @JsonProperty("nodeId")
    public String nodeId() {
        return nodeId;
    }

    @JsonProperty("type")
    public HumanTaskType type() {
        return type;
    }

    @JsonProperty("prompt")
    public String prompt() {
        return prompt;
    }

    @JsonProperty("context")
    public Map<String, Object> context() {
        return context;
    }

    @JsonProperty("createdAt")
    public Instant createdAt() {
        return createdAt;
    }

    @JsonProperty("expiresAt")
    public Instant expiresAt() {
        return expiresAt;
    }

    @JsonProperty("status")
    public HumanTaskStatus status() {
        return status;
    }

    @JsonProperty("response")
    public Map<String, Object> response() {
        return response;
    }

    @JsonProperty("completedAt")
    public Instant completedAt() {
        return completedAt;
    }

    public boolean isPending() {
        return status == HumanTaskStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == HumanTaskStatus.COMPLETED;
    }

    public boolean isExpired() {
        return status == HumanTaskStatus.EXPIRED || 
               (expiresAt != null && Instant.now().isAfter(expiresAt));
    }

    public HumanTask withResponse(Map<String, Object> response) {
        return new HumanTask(taskId, executionId, nodeId, type, prompt, context,
                createdAt, expiresAt, HumanTaskStatus.COMPLETED, response, Instant.now());
    }

    public HumanTask withStatus(HumanTaskStatus status) {
        return new HumanTask(taskId, executionId, nodeId, type, prompt, context,
                createdAt, expiresAt, status, response, 
                status == HumanTaskStatus.COMPLETED ? Instant.now() : completedAt);
    }

    public static final class Builder {
        private String taskId;
        private String executionId;
        private String nodeId;
        private HumanTaskType type;
        private String prompt;
        private Map<String, Object> context = new HashMap<>();
        private Instant createdAt;
        private Instant expiresAt;
        private HumanTaskStatus status = HumanTaskStatus.PENDING;

        private Builder() {
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public Builder type(HumanTaskType type) {
            this.type = type;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = new HashMap<>(context);
            return this;
        }

        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder timeout(Duration timeout) {
            if (timeout != null) {
                this.expiresAt = (createdAt != null ? createdAt : Instant.now()).plus(timeout);
            }
            return this;
        }

        public HumanTask build() {
            if (taskId == null) {
                taskId = java.util.UUID.randomUUID().toString();
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new HumanTask(taskId, executionId, nodeId, type, prompt, context,
                    createdAt, expiresAt, status, Map.of(), null);
        }
    }
}
