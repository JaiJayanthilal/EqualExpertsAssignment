package org.service;

import com.sun.net.httpserver.HttpServer;
import org.exception.PriceLookupException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the client against a local HTTP server, so no real network calls are made.
 */
class PriceApiClientServiceTest {

    private HttpServer server;
    private final List<String> requestedPaths = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnPriceFromApi() throws Exception {
        PriceApiClientService client = clientFor(200, "{\"title\": \"Corn Flakes\", \"price\": 2.52}");

        assertEquals(new BigDecimal("2.52"), client.getPrice("cornflakes"));
        assertEquals(List.of("/cornflakes.json"), requestedPaths);
    }

    @Test
    void shouldFailWithClearMessageForUnknownProduct() throws Exception {
        PriceApiClientService client = clientFor(404, "Not Found");

        PriceLookupException ex = assertThrows(PriceLookupException.class,
                () -> client.getPrice("unknown"));

        assertEquals("Unknown product: unknown", ex.getMessage());
    }

    @Test
    void shouldFailOnErrorStatus() throws Exception {
        PriceApiClientService client = clientFor(500, "Server Error");

        PriceLookupException ex = assertThrows(PriceLookupException.class,
                () -> client.getPrice("cornflakes"));

        assertTrue(ex.getMessage().contains("HTTP status 500"));
    }

    @Test
    void shouldFailWhenPriceIsMissing() throws Exception {
        for (String body : new String[]{"{}", "{\"title\": \"Corn Flakes\"}", ""}) {
            PriceApiClientService client = clientFor(200, body);

            PriceLookupException ex = assertThrows(PriceLookupException.class,
                    () -> client.getPrice("cornflakes"), "body: " + body);

            assertTrue(ex.getMessage().contains("Price missing"));
            server.stop(0);
        }
    }

    @Test
    void shouldFailOnNegativePrice() throws Exception {
        PriceApiClientService client = clientFor(200, "{\"price\": -1.00}");

        PriceLookupException ex = assertThrows(PriceLookupException.class,
                () -> client.getPrice("cornflakes"));

        assertTrue(ex.getMessage().contains("Negative price"));
    }

    @Test
    void shouldTimeOutWhenServerIsSlow() throws Exception {
        PriceApiClientService client = clientFor(200, "{\"price\": 2.52}", 2000, 200); // server answers after 2 sec

        // read timeout is 200 ms, so the call gives up long before the server answers
        assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                assertThrows(SocketTimeoutException.class, () -> client.getPrice("cornflakes")));
    }

    @Test
    void shouldRejectUnsafeProductNamesWithoutCallingApi() throws Exception {
        PriceApiClientService client = clientFor(200, "{\"price\": 2.52}");

        for (String product : new String[]{null, "", "../foo", "corn flakes", "cornflakes?x=1"}) {
            assertThrows(IllegalArgumentException.class, () -> client.getPrice(product));
        }

        assertTrue(requestedPaths.isEmpty());
    }

    private PriceApiClientService clientFor(int status, String body) throws IOException {
        return clientFor(status, body, 0, 1000);
    }

    private PriceApiClientService clientFor(int status, String body, long delayMs, int readTimeoutMs) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
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

        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/";
        return new PriceApiClientService(baseUrl, 1000, readTimeoutMs);
    }
}
