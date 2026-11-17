package org.service;

/**
 * The price API rejected the request itself (HTTP 4xx, e.g. unknown product).
 * Retrying won't help and the service isn't failing, so this is not retried
 * and does not count towards the circuit breaker.
 */
public class PriceClientException extends RuntimeException {

    private final int status;

    public PriceClientException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
