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

    // Result of asking the breaker for permission. TRIAL means the caller holds the
    // single HALF_OPEN probe and must make exactly one call before recording the outcome.
    public enum Permission {
        REJECTED,
        ALLOWED,
        TRIAL
    }

    /**
     * Handed out by acquirePermission() and passed back to recordSuccess()/recordFailure().
     * The generation identifies the breaker state the permit was granted in, so a result
     * that arrives after the state has changed (a stale result) is ignored.
     */
    public static final class Permit {
        private final Permission type;
        private final long generation;

        private Permit(Permission type, long generation) {
            this.type = type;
            this.generation = generation;
        }

        public Permission getType() {
            return type;
        }
    }

    private State state = State.CLOSED;

    // Incremented on every state change; permits from an older generation are stale
    private long generation = 0;

    private int failureCount = 0;
    private final int failureThreshold;
    private final long retryTimeout;

    private long openedAt = 0;

    // 🔥 Key addition: restrict HALF_OPEN to single request
    private boolean halfOpenRequestInProgress = false;

    public CircuitBreaker(int failureThreshold, long retryTimeout) {
        this.failureThreshold = failureThreshold;
        this.retryTimeout = retryTimeout;
    }

    public synchronized Permit acquirePermission() {

        // If OPEN → check if we can move to HALF_OPEN
        if (state == State.OPEN) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - openedAt > retryTimeout) {
                transitionTo(State.HALF_OPEN);
            } else {
                return permit(Permission.REJECTED); // still OPEN → reject
            }
        }

        // If HALF_OPEN → allow only ONE request
        if (state == State.HALF_OPEN) {
            if (!halfOpenRequestInProgress) {
                halfOpenRequestInProgress = true;
                return permit(Permission.TRIAL); // allow single trial request
            } else {
                return permit(Permission.REJECTED); // block others
            }
        }

        // CLOSED → allow all
        return permit(Permission.ALLOWED);
    }

    public synchronized void recordSuccess(Permit permit) {
        if (isStale(permit)) {
            return;
        }

        if (state == State.HALF_OPEN) {
            transitionTo(State.CLOSED); // trial succeeded
        } else {
            // CLOSED: stay in the same generation so other in-flight calls still count
            failureCount = 0;
        }
    }

    public synchronized void recordFailure(Permit permit) {
        if (isStale(permit)) {
            return;
        }

        // Trial failed OR threshold reached → OPEN
        if (state == State.HALF_OPEN || ++failureCount >= failureThreshold) {
            openedAt = System.currentTimeMillis();
            transitionTo(State.OPEN);
        }
    }

    /**
     * Give the permit back without recording a success or failure, for calls whose
     * outcome says nothing about the service's health (e.g. HTTP 4xx).
     * Frees the HALF_OPEN trial so the next caller can probe; otherwise does nothing.
     */
    public synchronized void release(Permit permit) {
        if (isStale(permit)) {
            return;
        }

        if (permit.type == Permission.TRIAL) {
            halfOpenRequestInProgress = false;
        }
    }

    // Results from a REJECTED permit or from an earlier state are ignored
    private boolean isStale(Permit permit) {
        return permit.type == Permission.REJECTED || permit.generation != generation;
    }

    private Permit permit(Permission type) {
        return new Permit(type, generation);
    }

    private void transitionTo(State newState) {
        state = newState;
        generation++;
        failureCount = 0;
        halfOpenRequestInProgress = false;
    }
}
