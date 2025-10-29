package dev.langgraph.core.execution;

import dev.langgraph.core.State;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ExecutionResult {
    
    private final String executionId;
    private final State finalState;
    private final ExecutionPhase phase;
    private final Instant startTime;
    private final Instant endTime;
    private final Throwable error;

    private ExecutionResult(String executionId, State finalState, ExecutionPhase phase, 
                           Instant startTime, Instant endTime, Throwable error) {
        this.executionId = Objects.requireNonNull(executionId);
        this.finalState = finalState;
        this.phase = Objects.requireNonNull(phase);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = endTime;
        this.error = error;
    }

    public static ExecutionResult success(String executionId, State finalState, Instant startTime, Instant endTime) {
        return new ExecutionResult(executionId, finalState, ExecutionPhase.COMPLETED, startTime, endTime, null);
    }

    public static ExecutionResult failed(String executionId, State lastState, Instant startTime, Instant endTime, Throwable error) {
        return new ExecutionResult(executionId, lastState, ExecutionPhase.FAILED, startTime, endTime, error);
    }

    public static ExecutionResult cancelled(String executionId, State lastState, Instant startTime, Instant endTime) {
        return new ExecutionResult(executionId, lastState, ExecutionPhase.CANCELLED, startTime, endTime, null);
    }

    public String executionId() {
        return executionId;
    }

    public State finalState() {
        return finalState;
    }

    public ExecutionPhase phase() {
        return phase;
    }

    public Instant startTime() {
        return startTime;
    }

    public Optional<Instant> endTime() {
        return Optional.ofNullable(endTime);
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    public Duration duration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }

    public boolean isSuccess() {
        return phase == ExecutionPhase.COMPLETED;
    }

    public boolean isFailed() {
        return phase == ExecutionPhase.FAILED;
    }

    public boolean isCancelled() {
        return phase == ExecutionPhase.CANCELLED;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
                "executionId='" + executionId + '\'' +
                ", phase=" + phase +
                ", duration=" + duration() +
                ", hasError=" + (error != null) +
                '}';
    }
}
