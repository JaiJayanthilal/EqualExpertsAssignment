package org.designprinciple;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.time.Duration;

/**
 * * This CIRCUIT BREAKER makes use of resilience4j library
 * */

public class ResilienceConfig {

    public static CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(1)
                .slidingWindowSize(10)
                .build();

        return CircuitBreaker.of("priceService", cbConfig);
    }

    public static Retry createRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                .build();

        return Retry.of("priceServiceRetry", retryConfig);
    }
}
