package dev.langgraph.ui.service;

import dev.langgraph.core.State;
import dev.langgraph.core.event.GraphEvent;
import dev.langgraph.ui.model.ExecutionEventDTO;
import dev.langgraph.ui.model.ExecutionStatus;
import dev.langgraph.ui.model.NodeStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ExecutionTracker {

    private final Map<String, ExecutionInfo> executions = new ConcurrentHashMap<>();

    public void startExecution(String executionId, String graphId, State initialState) {
        ExecutionInfo info = new ExecutionInfo(executionId, graphId, initialState);
        executions.put(executionId, info);
    }

    public void recordEvent(String executionId, GraphEvent event) {
        ExecutionInfo info = executions.get(executionId);
        if (info != null) {
            info.addEvent(event);
            updateStatusFromEvent(info, event);
        }
    }

    public void updateNodeStatus(String executionId, String nodeId, NodeStatus status) {
        ExecutionInfo info = executions.get(executionId);
        if (info != null) {
            info.nodeStatuses.put(nodeId, status);
            if (status == NodeStatus.RUNNING) {
                info.currentNode = nodeId;
            }
        }
    }

    public void updateExecutionStatus(String executionId, ExecutionStatus status) {
        ExecutionInfo info = executions.get(executionId);
        if (info != null) {
            info.status = status;
            if (status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED || status == ExecutionStatus.CANCELLED) {
                info.endTime = Instant.now();
            }
        }
    }

    public void updateState(String executionId, State state) {
        ExecutionInfo info = executions.get(executionId);
        if (info != null) {
            info.currentState = state;
        }
    }

    public void setError(String executionId, String error) {
        ExecutionInfo info = executions.get(executionId);
        if (info != null) {
            info.error = error;
        }
    }

    public Optional<ExecutionInfo> getExecution(String executionId) {
        return Optional.ofNullable(executions.get(executionId));
    }

    public List<ExecutionInfo> getAllExecutions() {
        return new ArrayList<>(executions.values());
    }

    public void removeExecution(String executionId) {
        executions.remove(executionId);
    }

    private void updateStatusFromEvent(ExecutionInfo info, GraphEvent event) {
        switch (event) {
            case GraphEvent.GraphExecutionStarted e -> {
                info.status = ExecutionStatus.RUNNING;
            }
            case GraphEvent.GraphExecutionCompleted e -> {
                info.status = ExecutionStatus.COMPLETED;
                info.endTime = Instant.now();
            }
            case GraphEvent.GraphExecutionFailed e -> {
                info.status = ExecutionStatus.FAILED;
                info.endTime = Instant.now();
                info.error = e.error().getMessage();
            }
            case GraphEvent.NodeExecutionStarted e -> {
                info.nodeStatuses.put(e.nodeId(), NodeStatus.RUNNING);
                info.currentNode = e.nodeId();
            }
            case GraphEvent.NodeExecutionCompleted e -> {
                info.nodeStatuses.put(e.nodeId(), NodeStatus.COMPLETED);
            }
            case GraphEvent.NodeExecutionFailed e -> {
                info.nodeStatuses.put(e.nodeId(), NodeStatus.FAILED);
            }
            default -> {
            }
        }
    }

    public static class ExecutionInfo {
        private final String executionId;
        private final String graphId;
        private final Instant startTime;
        private final List<ExecutionEventDTO> events;
        private final Map<String, NodeStatus> nodeStatuses;
        private State currentState;
        private ExecutionStatus status;
        private Instant endTime;
        private String currentNode;
        private String error;

        public ExecutionInfo(String executionId, String graphId, State initialState) {
            this.executionId = executionId;
            this.graphId = graphId;
            this.currentState = initialState;
            this.startTime = Instant.now();
            this.status = ExecutionStatus.PENDING;
            this.events = new CopyOnWriteArrayList<>();
            this.nodeStatuses = new ConcurrentHashMap<>();
        }

        public void addEvent(GraphEvent event) {
            ExecutionEventDTO dto = new ExecutionEventDTO(
                    event.getClass().getSimpleName(),
                    event.executionId(),
                    extractNodeId(event),
                    event.timestamp(),
                    event.metadata()
            );
            events.add(dto);
        }

        private String extractNodeId(GraphEvent event) {
            return switch (event) {
                case GraphEvent.NodeExecutionStarted e -> e.nodeId();
                case GraphEvent.NodeExecutionCompleted e -> e.nodeId();
                case GraphEvent.NodeExecutionFailed e -> e.nodeId();
                case GraphEvent.StateChanged e -> e.nodeId();
                case GraphEvent.HumanTaskCreated e -> e.nodeId();
                case GraphEvent.HumanTaskCompleted e -> e.nodeId();
                case GraphEvent.HumanTaskExpired e -> e.nodeId();
                case GraphEvent.HumanTaskCancelled e -> e.nodeId();
                case GraphEvent.HumanTaskEscalated e -> e.nodeId();
                default -> null;
            };
        }

        public String getExecutionId() {
            return executionId;
        }

        public String getGraphId() {
            return graphId;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public List<ExecutionEventDTO> getEvents() {
            return new ArrayList<>(events);
        }

        public Map<String, NodeStatus> getNodeStatuses() {
            return new HashMap<>(nodeStatuses);
        }

        public State getCurrentState() {
            return currentState;
        }

        public ExecutionStatus getStatus() {
            return status;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public String getCurrentNode() {
            return currentNode;
        }

        public String getError() {
            return error;
        }
    }
}
