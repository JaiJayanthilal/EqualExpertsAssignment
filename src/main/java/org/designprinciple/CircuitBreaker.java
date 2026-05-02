package org.designprinciple;


/**
* * This is manually created CIRCUIT BREAKER
* */

public class CircuitBreaker {

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private State state = State.CLOSED;

    private int failureCount = 0;
    private final int failureThreshold;
    private final long retryTimeout;

    private long lastFailureTime = 0;

    // 🔥 Key addition: restrict HALF_OPEN to single request
    private boolean halfOpenRequestInProgress = false;

    public CircuitBreaker(int failureThreshold, long retryTimeout) {
        this.failureThreshold = failureThreshold;
        this.retryTimeout = retryTimeout;
    }

    public synchronized boolean allowRequest() {

        // If OPEN → check if we can move to HALF_OPEN
        if (state == State.OPEN) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastFailureTime > retryTimeout) {
                state = State.HALF_OPEN;
                halfOpenRequestInProgress = false;
            } else {
                return false; // still OPEN → reject
            }
        }

        // If HALF_OPEN → allow only ONE request
        if (state == State.HALF_OPEN) {
            if (!halfOpenRequestInProgress) {
                halfOpenRequestInProgress = true;
                return true; // allow single trial request
            } else {
                return false; // block others
            }
        }

        // CLOSED → allow all
        return true;
    }

    public synchronized void recordSuccess() {
        failureCount = 0;
        state = State.CLOSED;
        halfOpenRequestInProgress = false;
    }

    public synchronized void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();

        // If already HALF_OPEN OR threshold reached → OPEN
        if (state == State.HALF_OPEN || failureCount >= failureThreshold) {
            state = State.OPEN;
        }

        halfOpenRequestInProgress = false;
    }
}