package dev.langgraph.core.execution;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void noRetryPolicyShouldNeverRetry() {
        RetryPolicy policy = RetryPolicy.noRetry();

        assertFalse(policy.shouldRetry(1, new RuntimeException()));
        assertFalse(policy.shouldRetry(2, new RuntimeException()));
        assertEquals(Duration.ZERO, policy.getBackoffDuration(1));
    }

    @Test
    void fixedRetryPolicyShouldRetryUpToMaxAttempts() {
        RetryPolicy policy = RetryPolicy.fixedRetry(3, Duration.ofMillis(100));

        assertTrue(policy.shouldRetry(1, new RuntimeException()));
        assertTrue(policy.shouldRetry(2, new RuntimeException()));
        assertFalse(policy.shouldRetry(3, new RuntimeException()));
        assertFalse(policy.shouldRetry(4, new RuntimeException()));
    }

    @Test
    void fixedRetryPolicyShouldReturnFixedBackoff() {
        RetryPolicy policy = RetryPolicy.fixedRetry(3, Duration.ofMillis(100));

        assertEquals(Duration.ofMillis(100), policy.getBackoffDuration(1));
        assertEquals(Duration.ofMillis(100), policy.getBackoffDuration(2));
        assertEquals(Duration.ofMillis(100), policy.getBackoffDuration(3));
    }

    @Test
    void exponentialBackoffShouldIncreaseDelay() {
        RetryPolicy policy = RetryPolicy.exponentialBackoff(5, Duration.ofMillis(100), 2.0);

        assertTrue(policy.shouldRetry(1, new RuntimeException()));
        assertTrue(policy.shouldRetry(4, new RuntimeException()));
        assertFalse(policy.shouldRetry(5, new RuntimeException()));

        assertEquals(100, policy.getBackoffDuration(1).toMillis());
        assertEquals(200, policy.getBackoffDuration(2).toMillis());
        assertEquals(400, policy.getBackoffDuration(3).toMillis());
        assertEquals(800, policy.getBackoffDuration(4).toMillis());
    }

    @Test
    void shouldThrowExceptionForInvalidMaxAttempts() {
        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.fixedRetry(0, Duration.ofMillis(100))
        );

        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.exponentialBackoff(0, Duration.ofMillis(100), 2.0)
        );
    }

    @Test
    void shouldThrowExceptionForInvalidMultiplier() {
        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.exponentialBackoff(3, Duration.ofMillis(100), 0.0)
        );

        assertThrows(IllegalArgumentException.class, () -> 
            RetryPolicy.exponentialBackoff(3, Duration.ofMillis(100), -1.0)
        );
    }

    @Test
    void shouldHandleNullBackoff() {
        assertThrows(NullPointerException.class, () -> 
            RetryPolicy.fixedRetry(3, null)
        );

        assertThrows(NullPointerException.class, () -> 
            RetryPolicy.exponentialBackoff(3, null, 2.0)
        );
    }
}
