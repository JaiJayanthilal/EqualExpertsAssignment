package org.designprinciple;

import org.designprinciple.CircuitBreaker.Permission;
import org.designprinciple.CircuitBreaker.Permit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void shouldAllowRequestsWhenClosed() {
        CircuitBreaker cb = new CircuitBreaker(3, 1000);

        assertEquals(Permission.ALLOWED, cb.acquirePermission().getType());
    }

    @Test
    void shouldOpenAfterFailures() {
        CircuitBreaker cb = new CircuitBreaker(2, 1000);

        cb.recordFailure(cb.acquirePermission());
        cb.recordFailure(cb.acquirePermission());

        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }

    @Test
    void shouldMoveToHalfOpenAfterTimeout() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure(cb.acquirePermission());

        Thread.sleep(150);

        assertEquals(Permission.TRIAL, cb.acquirePermission().getType()); // HALF_OPEN
    }

    @Test
    void shouldAllowOnlyOneRequestInHalfOpen() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure(cb.acquirePermission());
        Thread.sleep(150);

        assertEquals(Permission.TRIAL, cb.acquirePermission().getType());
        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }

    @Test
    void shouldGrantTrialPermissionInHalfOpen() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        Permit first = cb.acquirePermission();
        assertEquals(Permission.ALLOWED, first.getType());

        cb.recordFailure(first);
        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());

        Thread.sleep(150);

        assertEquals(Permission.TRIAL, cb.acquirePermission().getType());
        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }

    @Test
    void shouldCloseWhenTrialSucceeds() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure(cb.acquirePermission());
        Thread.sleep(150);

        cb.recordSuccess(cb.acquirePermission()); // TRIAL succeeds

        assertEquals(Permission.ALLOWED, cb.acquirePermission().getType());
    }

    @Test
    void shouldReopenWhenTrialFails() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure(cb.acquirePermission());
        Thread.sleep(150);

        cb.recordFailure(cb.acquirePermission()); // TRIAL fails

        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }

    @Test
    void shouldResetFailureCountOnSuccessWhenClosed() {
        CircuitBreaker cb = new CircuitBreaker(2, 1000);

        cb.recordFailure(cb.acquirePermission());
        cb.recordSuccess(cb.acquirePermission());
        cb.recordFailure(cb.acquirePermission());

        assertEquals(Permission.ALLOWED, cb.acquirePermission().getType()); // only 1 consecutive failure
    }

    @Test
    void shouldCountFailuresFromConcurrentCallsInSameClosedGeneration() {
        CircuitBreaker cb = new CircuitBreaker(2, 1000);

        Permit a = cb.acquirePermission();
        Permit b = cb.acquirePermission();

        cb.recordFailure(a);
        cb.recordFailure(b);

        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }

    // Case 1: a slow call granted while CLOSED succeeds during the HALF_OPEN trial
    @Test
    void shouldIgnoreStaleSuccessDuringTrial() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        Permit slow = cb.acquirePermission();          // generation 0, CLOSED
        cb.recordFailure(cb.acquirePermission());      // → OPEN
        Thread.sleep(150);

        Permit trial = cb.acquirePermission();
        assertEquals(Permission.TRIAL, trial.getType());

        cb.recordSuccess(slow); // stale → must not close the circuit

        assertEquals(Permission.REJECTED, cb.acquirePermission().getType()); // trial still in progress

        cb.recordFailure(trial); // only the trial decides → OPEN
        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }

    // Case 2: a slow call granted while CLOSED fails during the HALF_OPEN trial
    @Test
    void shouldIgnoreStaleFailureDuringTrial() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        Permit slow = cb.acquirePermission();          // generation 0, CLOSED
        cb.recordFailure(cb.acquirePermission());      // → OPEN
        Thread.sleep(150);

        Permit trial = cb.acquirePermission();
        assertEquals(Permission.TRIAL, trial.getType());

        cb.recordFailure(slow); // stale → must not re-open or reset the trial flag
        Thread.sleep(150);

        // still HALF_OPEN with the trial in progress → no second trial
        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());

        cb.recordSuccess(trial);
        assertEquals(Permission.ALLOWED, cb.acquirePermission().getType());
    }

    @Test
    void shouldFreeTrialOnReleaseWithoutChangingState() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(1, 100);

        cb.recordFailure(cb.acquirePermission()); // → OPEN
        Thread.sleep(150);

        Permit trial = cb.acquirePermission();
        assertEquals(Permission.TRIAL, trial.getType());

        cb.release(trial);

        // still HALF_OPEN, and the trial is available to the next caller
        assertEquals(Permission.TRIAL, cb.acquirePermission().getType());
    }

    @Test
    void shouldNotCountReleaseAsFailureWhenClosed() {
        CircuitBreaker cb = new CircuitBreaker(1, 1000);

        cb.release(cb.acquirePermission());

        assertEquals(Permission.ALLOWED, cb.acquirePermission().getType());
    }

    @Test
    void shouldIgnoreResultsFromRejectedPermits() {
        CircuitBreaker cb = new CircuitBreaker(1, 1000);

        cb.recordFailure(cb.acquirePermission()); // → OPEN

        Permit rejected = cb.acquirePermission();
        assertEquals(Permission.REJECTED, rejected.getType());

        cb.recordSuccess(rejected);

        assertEquals(Permission.REJECTED, cb.acquirePermission().getType());
    }
}
