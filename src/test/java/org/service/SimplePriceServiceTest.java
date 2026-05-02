package org.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimplePriceServiceTest {

    @Test
    void shouldReturnPriceOnSuccess() throws Exception {
        SimplePriceService service = spy(new SimplePriceService());

        doReturn(BigDecimal.TEN)
                .when(service)
                .fetchPrice("apple");

        BigDecimal result = service.getPrice("apple");

        assertEquals(BigDecimal.TEN, result);
    }

    @Test
    void shouldRetryAndSucceed() throws Exception {
        SimplePriceService service = spy(new SimplePriceService());

        doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doReturn(BigDecimal.TEN)
                .when(service)
                .fetchPrice("apple");

        BigDecimal result = service.getPrice("apple");

        assertEquals(BigDecimal.TEN, result);

        verify(service, times(3)).fetchPrice("apple"); // 2 fails + 1 success
    }

    @Test
    void shouldFailAfterMaxRetries() throws Exception {
        SimplePriceService service = spy(new SimplePriceService());

        doThrow(new RuntimeException())
                .when(service)
                .fetchPrice("apple");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getPrice("apple"));

        assertTrue(ex.getMessage().contains("Failed after retries"));

        verify(service, times(3)).fetchPrice("apple");
    }

    @Test
    void shouldOpenCircuitAfterFailures() throws Exception {
        SimplePriceService service = spy(new SimplePriceService());

        doThrow(new RuntimeException())
                .when(service)
                .fetchPrice("apple");

        // first call → triggers failures
        assertThrows(RuntimeException.class, () -> service.getPrice("apple"));

        // second call → circuit should be OPEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getPrice("apple"));

        assertTrue(ex.getMessage().contains("Circuit is OPEN"));
    }
}
