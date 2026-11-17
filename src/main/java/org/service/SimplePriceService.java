package org.service;

import com.google.gson.Gson;
import org.designprinciple.CircuitBreaker;
import org.model.PriceResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class SimplePriceService implements PriceProvider {

    private static final String BASE_URL =
            "https://equalexperts.github.io/backend-take-home-test-data/";

    // Product names are used as a URL path segment, so only allow safe characters
    private static final Pattern VALID_PRODUCT = Pattern.compile("[A-Za-z0-9_-]+");

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 200;

    // Without timeouts a hanging connection blocks the caller forever
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;

    private final Gson gson = new Gson();

    private final String baseUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    // Circuit Breaker instance
    private final CircuitBreaker circuitBreaker;

    public SimplePriceService() {
        this(new CircuitBreaker(3, 5000)); // 3 failed getPrice calls → OPEN, retry after 5 sec
    }

    SimplePriceService(CircuitBreaker circuitBreaker) {
        this(circuitBreaker, BASE_URL, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    SimplePriceService(CircuitBreaker circuitBreaker, String baseUrl,
                       int connectTimeoutMs, int readTimeoutMs) {
        this.circuitBreaker = circuitBreaker;
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * The whole retry sequence counts as ONE call to the circuit breaker:
     * success if any attempt succeeds, a single failure if all attempts fail.
     * A HALF_OPEN trial is not retried, so the probe makes exactly one HTTP call.
     * Client errors (4xx) fail immediately and don't affect the breaker.
     */
    @Override
    public BigDecimal getPrice(String product) throws Exception {
        if (product == null || !VALID_PRODUCT.matcher(product).matches()) {
            throw new IllegalArgumentException("Invalid product name: " + product);
        }

        CircuitBreaker.Permit permit = circuitBreaker.acquirePermission();

        if (permit.getType() == CircuitBreaker.Permission.REJECTED) {
            throw new RuntimeException("Circuit is OPEN / HALF_OPEN busy. Skipping API call");
        }

        int maxAttempts = permit.getType() == CircuitBreaker.Permission.TRIAL ? 1 : MAX_RETRIES;
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                BigDecimal price = fetchPrice(product);
                circuitBreaker.recordSuccess(permit);
                return price;
            } catch (PriceClientException e) {
                circuitBreaker.release(permit); // not the service's fault → no retry, no failure
                throw e;
            } catch (Exception e) {
                lastFailure = e;
            }

            if (attempt < maxAttempts) {
                Thread.sleep(RETRY_DELAY_MS);
            }
        }

        circuitBreaker.recordFailure(permit);
        throw new RuntimeException("Failed after retries", lastFailure);
    }

    protected BigDecimal fetchPrice(String product) throws Exception {

        String urlStr = baseUrl + product + ".json";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);

        try {
            int status = conn.getResponseCode();

            if (isClientError(status)) {
                throw new PriceClientException(
                        "Price request rejected for product " + product + ", HTTP status: " + status, status);
            }
            if (status != 200) {
                throw new RuntimeException("Failed with HTTP status: " + status);
            }

            PriceResponse response;
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                response = gson.fromJson(reader, PriceResponse.class);
            }

            // Fail here rather than letting a null price surface later in the cart
            if (response == null || response.getPrice() == null) {
                throw new RuntimeException("Price missing in response for product: " + product);
            }
            if (response.getPrice().signum() < 0) {
                throw new RuntimeException("Negative price in response for product: " + product);
            }
            return response.getPrice();
        } finally {
            conn.disconnect();
        }
    }

    // 408 (timeout) and 429 (rate limited) are 4xx but temporary, so they stay retryable
    private static boolean isClientError(int status) {
        return status >= 400 && status < 500 && status != 408 && status != 429;
    }
}
