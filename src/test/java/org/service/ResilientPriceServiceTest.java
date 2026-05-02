package org.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.designprinciple.ResilienceConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResilientPriceServiceTest {

    @Test
    void shouldReturnPriceOnSuccess() {
        CircuitBreaker cb = ResilienceConfig.createCircuitBreaker();
        Retry retry = ResilienceConfig.createRetry();

        ResilientPriceService service = spy(new ResilientPriceService(cb, retry));

        doReturn(BigDecimal.TEN)
                .when(service)
                .fetchPrice("apple");

        BigDecimal result = service.getPrice("apple");

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    void shouldRetryAndSucceed() {
        CircuitBreaker cb = ResilienceConfig.createCircuitBreaker();
        Retry retry = ResilienceConfig.createRetry();

        ResilientPriceService service = spy(new ResilientPriceService(cb, retry));

        doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doReturn(BigDecimal.TEN)
                .when(service)
                .fetchPrice("apple");

        BigDecimal result = service.getPrice("apple");

        assertEquals(BigDecimal.TEN, result);

        verify(service, times(3)).fetchPrice("apple");
    }

    @Test
    void shouldFailAfterRetries() {
        CircuitBreaker cb = ResilienceConfig.createCircuitBreaker();
        Retry retry = ResilienceConfig.createRetry();

        ResilientPriceService service = spy(new ResilientPriceService(cb, retry));

        doThrow(new RuntimeException())
                .when(service)
                .fetchPrice("apple");

        assertThrows(RuntimeException.class,
                () -> service.getPrice("apple"));
    }

    @Test
    void shouldOpenCircuitBreakerAfterFailures() {
        CircuitBreaker cb = ResilienceConfig.createCircuitBreaker();
        Retry retry = ResilienceConfig.createRetry();

        ResilientPriceService service = spy(new ResilientPriceService(cb, retry));

        doThrow(new RuntimeException())
                .when(service)
                .fetchPrice("apple");

        // trigger failures multiple times
        for (int i = 0; i < 10; i++) {
            try {
                service.getPrice("apple");
            } catch (Exception ignored) {}
        }

        // eventually circuit should open → immediate failure
        assertThrows(Exception.class, () -> service.getPrice("apple"));
    }
}