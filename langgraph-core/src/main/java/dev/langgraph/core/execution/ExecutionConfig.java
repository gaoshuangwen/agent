package dev.langgraph.core.execution;

import java.time.Duration;
import java.util.Objects;

public final class ExecutionConfig {
    
    private final int maxConcurrency;
    private final Duration timeout;
    private final RetryPolicy retryPolicy;
    private final SnapshotStore snapshotStore;
    private final boolean enableSnapshots;

    private ExecutionConfig(int maxConcurrency, Duration timeout, RetryPolicy retryPolicy, 
                           SnapshotStore snapshotStore, boolean enableSnapshots) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("Max concurrency must be at least 1");
        }
        this.maxConcurrency = maxConcurrency;
        this.timeout = Objects.requireNonNull(timeout);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        this.snapshotStore = Objects.requireNonNull(snapshotStore);
        this.enableSnapshots = enableSnapshots;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExecutionConfig defaults() {
        return builder().build();
    }

    public int maxConcurrency() {
        return maxConcurrency;
    }

    public Duration timeout() {
        return timeout;
    }

    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    public SnapshotStore snapshotStore() {
        return snapshotStore;
    }

    public boolean enableSnapshots() {
        return enableSnapshots;
    }

    public static final class Builder {
        private int maxConcurrency = Runtime.getRuntime().availableProcessors();
        private Duration timeout = Duration.ofMinutes(10);
        private RetryPolicy retryPolicy = RetryPolicy.noRetry();
        private SnapshotStore snapshotStore = SnapshotStore.noOp();
        private boolean enableSnapshots = false;

        private Builder() {
        }

        public Builder maxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout);
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy);
            return this;
        }

        public Builder snapshotStore(SnapshotStore snapshotStore) {
            this.snapshotStore = Objects.requireNonNull(snapshotStore);
            return this;
        }

        public Builder enableSnapshots(boolean enableSnapshots) {
            this.enableSnapshots = enableSnapshots;
            return this;
        }

        public ExecutionConfig build() {
            if (enableSnapshots && snapshotStore instanceof NoOpSnapshotStore) {
                snapshotStore = SnapshotStore.inMemory();
            }
            return new ExecutionConfig(maxConcurrency, timeout, retryPolicy, snapshotStore, enableSnapshots);
        }
    }
}
