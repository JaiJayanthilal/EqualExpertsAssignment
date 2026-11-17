package org.service;

import com.sun.net.httpserver.HttpServer;
import org.designprinciple.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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

        // threshold is 3 failed getPrice calls, each one retried 3 times
        for (int i = 0; i < 3; i++) {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.getPrice("apple"));
            assertTrue(ex.getMessage().contains("Failed after retries"));
        }

        // fourth call → circuit should be OPEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getPrice("apple"));

        assertTrue(ex.getMessage().contains("Circuit is OPEN"));
        verify(service, times(9)).fetchPrice("apple");
    }

    @Test
    void shouldCountWholeRetrySequenceAsOneFailure() throws Exception {
        SimplePriceService service = spy(new SimplePriceService(new CircuitBreaker(2, 5000)));

        doThrow(new RuntimeException())
                .when(service)
                .fetchPrice("apple");

        // first call: 3 failed attempts count as 1 breaker failure, so circuit stays CLOSED
        assertThrows(RuntimeException.class, () -> service.getPrice("apple"));

        // second call is still allowed through and fully retried
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getPrice("apple"));
        assertTrue(ex.getMessage().contains("Failed after retries"));

        verify(service, times(6)).fetchPrice("apple");
    }

    @Test
    void shouldMakeSingleCallWhenHalfOpenTrialFails() throws Exception {
        SimplePriceService service = spy(new SimplePriceService(new CircuitBreaker(1, 100)));

        doThrow(new RuntimeException())
                .when(service)
                .fetchPrice("apple");

        // open the circuit
        assertThrows(RuntimeException.class, () -> service.getPrice("apple"));
        clearInvocations(service);

        Thread.sleep(150); // wait for HALF_OPEN

        // trial call fails → exactly one HTTP call, no retries
        assertThrows(RuntimeException.class, () -> service.getPrice("apple"));
        verify(service, times(1)).fetchPrice("apple");

        // circuit is OPEN again
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getPrice("apple"));
        assertTrue(ex.getMessage().contains("Circuit is OPEN"));
        verify(service, times(1)).fetchPrice("apple");
    }

    @Test
    void shouldCloseCircuitWhenHalfOpenTrialSucceeds() throws Exception {
        SimplePriceService service = spy(new SimplePriceService(new CircuitBreaker(1, 100)));

        doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doReturn(BigDecimal.TEN)
                .when(service)
                .fetchPrice("apple");

        // open the circuit
        assertThrows(RuntimeException.class, () -> service.getPrice("apple"));
        Thread.sleep(150); // wait for HALF_OPEN

        assertEquals(BigDecimal.TEN, service.getPrice("apple")); // trial succeeds → CLOSED
        assertEquals(BigDecimal.TEN, service.getPrice("apple")); // normal call allowed

        verify(service, times(5)).fetchPrice("apple");
    }

    @Test
    void shouldReleaseTrialOnClientError() throws Exception {
        SimplePriceService service = spy(new SimplePriceService(new CircuitBreaker(1, 100)));

        doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doThrow(new PriceClientException("not found", 404))
                .doReturn(BigDecimal.TEN)
                .when(service)
                .fetchPrice("apple");

        // open the circuit
        assertThrows(RuntimeException.class, () -> service.getPrice("apple"));
        Thread.sleep(150); // wait for HALF_OPEN

        // trial hits a 404 → says nothing about the service, so the trial is freed
        assertThrows(PriceClientException.class, () -> service.getPrice("apple"));

        // next caller gets the trial (not "Circuit is OPEN"), succeeds and closes the circuit
        assertEquals(BigDecimal.TEN, service.getPrice("apple"));
        verify(service, times(5)).fetchPrice("apple");
    }

    @Test
    void shouldRejectInvalidProductNameWithoutCallingApi() throws Exception {
        SimplePriceService service = spy(new SimplePriceService());

        for (String product : new String[]{null, "", "../foo", "corn flakes", "cornflakes?x=1"}) {
            assertThrows(IllegalArgumentException.class, () -> service.getPrice(product));
        }

        verify(service, never()).fetchPrice(anyString());
    }

    @Test
    void shouldNotRetryOrTripBreakerOn404() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = startServer(0, 404, "Not Found", hits);
        try {
            // threshold 1: a single counted failure would open the circuit
            SimplePriceService service = new SimplePriceService(
                    new CircuitBreaker(1, 5000), baseUrl(server), 1000, 1000);

            PriceClientException ex = assertThrows(PriceClientException.class,
                    () -> service.getPrice("unknown"));
            assertEquals(404, ex.getStatus());

            // circuit still CLOSED → the next call reaches the server again
            assertThrows(PriceClientException.class, () -> service.getPrice("unknown"));

            assertEquals(2, hits.get()); // one HTTP call per getPrice, no retries
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldTreatServerErrorsAndTemporary4xxAsRetryable() throws Exception {
        for (int status : new int[]{500, 503, 408, 429}) {
            HttpServer server = startServer(0, status, "error", new AtomicInteger());
            try {
                SimplePriceService service = new SimplePriceService(
                        new CircuitBreaker(3, 5000), baseUrl(server), 1000, 1000);

                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.fetchPrice("cornflakes"));
                assertFalse(ex instanceof PriceClientException, "status " + status);
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void shouldFailFastWhenPriceIsMissing() throws Exception {
        for (String body : new String[]{"{}", "{\"title\": \"Corn Flakes\"}", ""}) {
            HttpServer server = startServer(0, 200, body, new AtomicInteger());
            try {
                SimplePriceService service = new SimplePriceService(
                        new CircuitBreaker(3, 5000), baseUrl(server), 1000, 1000);

                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> service.fetchPrice("cornflakes"));
                assertTrue(ex.getMessage().contains("Price missing"), "body: " + body);
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void shouldRejectNegativePrice() throws Exception {
        HttpServer server = startServer(0, 200, "{\"price\": -1.00}", new AtomicInteger());
        try {
            SimplePriceService service = new SimplePriceService(
                    new CircuitBreaker(3, 5000), baseUrl(server), 1000, 1000);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.fetchPrice("cornflakes"));
            assertTrue(ex.getMessage().contains("Negative price"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldFetchPriceFromServer() throws Exception {
        HttpServer server = startServer(0, 200, "{\"price\": 2.52}", new AtomicInteger());
        try {
            SimplePriceService service = new SimplePriceService(
                    new CircuitBreaker(3, 5000), baseUrl(server), 1000, 1000);

            assertEquals(new BigDecimal("2.52"), service.fetchPrice("cornflakes"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldTimeOutWhenServerIsSlow() throws Exception {
        HttpServer server = startServer(2000, 200, "{\"price\": 2.52}", new AtomicInteger()); // responds after 2 sec
        try {
            SimplePriceService service = new SimplePriceService(
                    new CircuitBreaker(3, 5000), baseUrl(server), 1000, 200);

            // read timeout is 200 ms, so the call gives up long before the server answers
            assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                    assertThrows(SocketTimeoutException.class,
                            () -> service.fetchPrice("cornflakes")));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(long delayMs, int status, String body,
                                          AtomicInteger hits) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length); // -1 = no body
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static String baseUrl(HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }
}
