package dev.langgraph.core.execution;

import java.time.Duration;
import java.util.Objects;

public interface RetryPolicy {
    
    boolean shouldRetry(int attemptNumber, Throwable error);
    
    Duration getBackoffDuration(int attemptNumber);
    
    static RetryPolicy noRetry() {
        return new NoRetryPolicy();
    }
    
    static RetryPolicy fixedRetry(int maxAttempts, Duration backoff) {
        return new FixedRetryPolicy(maxAttempts, backoff);
    }
    
    static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialBackoff, double multiplier) {
        return new ExponentialBackoffRetryPolicy(maxAttempts, initialBackoff, multiplier);
    }

    final class NoRetryPolicy implements RetryPolicy {
        @Override
        public boolean shouldRetry(int attemptNumber, Throwable error) {
            return false;
        }

        @Override
        public Duration getBackoffDuration(int attemptNumber) {
            return Duration.ZERO;
        }
    }

    final class FixedRetryPolicy implements RetryPolicy {
        private final int maxAttempts;
        private final Duration backoff;

        public FixedRetryPolicy(int maxAttempts, Duration backoff) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("Max attempts must be at least 1");
            }
            this.maxAttempts = maxAttempts;
            this.backoff = Objects.requireNonNull(backoff);
        }

        @Override
        public boolean shouldRetry(int attemptNumber, Throwable error) {
            return attemptNumber < maxAttempts;
        }

        @Override
        public Duration getBackoffDuration(int attemptNumber) {
            return backoff;
        }
    }

    final class ExponentialBackoffRetryPolicy implements RetryPolicy {
        private final int maxAttempts;
        private final Duration initialBackoff;
        private final double multiplier;

        public ExponentialBackoffRetryPolicy(int maxAttempts, Duration initialBackoff, double multiplier) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("Max attempts must be at least 1");
            }
            if (multiplier <= 0) {
                throw new IllegalArgumentException("Multiplier must be positive");
            }
            this.maxAttempts = maxAttempts;
            this.initialBackoff = Objects.requireNonNull(initialBackoff);
            this.multiplier = multiplier;
        }

        @Override
        public boolean shouldRetry(int attemptNumber, Throwable error) {
            return attemptNumber < maxAttempts;
        }

        @Override
        public Duration getBackoffDuration(int attemptNumber) {
            long backoffMillis = (long) (initialBackoff.toMillis() * Math.pow(multiplier, attemptNumber - 1));
            return Duration.ofMillis(backoffMillis);
        }
    }
}
