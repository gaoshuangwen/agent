package dev.langgraph.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Checkpoint {
    
    private final String checkpointId;
    private final String graphId;
    private final String executionId;
    private final Map<String, Object> state;
    private final String currentNodeId;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    @JsonCreator
    private Checkpoint(@JsonProperty("checkpointId") String checkpointId, 
                      @JsonProperty("graphId") String graphId, 
                      @JsonProperty("executionId") String executionId, 
                      @JsonProperty("state") Map<String, Object> state, 
                      @JsonProperty("currentNodeId") String currentNodeId, 
                      @JsonProperty("timestamp") Instant timestamp, 
                      @JsonProperty("metadata") Map<String, Object> metadata) {
        this.checkpointId = Objects.requireNonNull(checkpointId, "Checkpoint ID cannot be null");
        this.graphId = Objects.requireNonNull(graphId, "Graph ID cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "Execution ID cannot be null");
        this.state = state == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(state));
        this.currentNodeId = currentNodeId;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("checkpointId")
    public String checkpointId() {
        return checkpointId;
    }

    @JsonProperty("graphId")
    public String graphId() {
        return graphId;
    }

    @JsonProperty("executionId")
    public String executionId() {
        return executionId;
    }

    @JsonProperty("state")
    public Map<String, Object> state() {
        return state;
    }

    @JsonProperty("currentNodeId")
    public String currentNodeId() {
        return currentNodeId;
    }

    @JsonProperty("timestamp")
    public Instant timestamp() {
        return timestamp;
    }

    @JsonProperty("metadata")
    public Map<String, Object> metadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Checkpoint that = (Checkpoint) o;
        return Objects.equals(checkpointId, that.checkpointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkpointId);
    }

    @Override
    public String toString() {
        return "Checkpoint{" +
                "checkpointId='" + checkpointId + '\'' +
                ", graphId='" + graphId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", currentNodeId='" + currentNodeId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public static final class Builder {
        private String checkpointId;
        private String graphId;
        private String executionId;
        private Map<String, Object> state = new HashMap<>();
        private String currentNodeId;
        private Instant timestamp;
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder checkpointId(String checkpointId) {
            this.checkpointId = checkpointId;
            return this;
        }

        public Builder graphId(String graphId) {
            this.graphId = graphId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder state(Map<String, Object> state) {
            this.state = new HashMap<>(state);
            return this;
        }

        public Builder currentNodeId(String currentNodeId) {
            this.currentNodeId = currentNodeId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Checkpoint build() {
            if (checkpointId == null) {
                checkpointId = java.util.UUID.randomUUID().toString();
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            return new Checkpoint(checkpointId, graphId, executionId, state, 
                                currentNodeId, timestamp, metadata);
        }
    }
}
