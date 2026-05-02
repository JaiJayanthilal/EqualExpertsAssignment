package org.designprinciple;

import org.designprinciple.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void shouldAllowRequestsWhenClosed() {
        CircuitBreaker cb = new CircuitBreaker(3, 1000);

        assertTrue(cb.allowRequest());
    }

    @Test
    void shouldOpenAfterFailures() {
        CircuitBreaker cb = new CircuitBreaker(2, 1000);

        cb.recordFailure();
        cb.recordFailure();

        assertFalse(cb.allowRequest());
    }

    @Test
    void shouldMoveToHalfOpenAfterTimeout() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure();

        Thread.sleep(150);

        assertTrue(cb.allowRequest()); // HALF_OPEN
    }

    @Test
    void shouldAllowOnlyOneRequestInHalfOpen() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure();
        Thread.sleep(150);

        assertTrue(cb.allowRequest());
        assertFalse(cb.allowRequest());
    }

    @Test
    void shouldCloseOnSuccess() {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure();
        cb.recordSuccess();

        assertTrue(cb.allowRequest());
    }
}
