package org.service;

import com.google.gson.Gson;
import org.model.PriceResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Fetches product prices from the Equal Experts price API.
 */
public class PriceApiClient implements PriceProvider {

    private static final String BASE_URL =
            "https://equalexperts.github.io/backend-take-home-test-data/";

    // Product names are used as a URL path segment, so only allow safe characters
    private static final Pattern VALID_PRODUCT = Pattern.compile("[A-Za-z0-9_-]+");

    // Without timeouts a hanging connection blocks the caller forever
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;

    private final Gson gson = new Gson();

    private final String baseUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public PriceApiClient() {
        this(BASE_URL, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    PriceApiClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public BigDecimal getPrice(String product) throws IOException {
        if (product == null || !VALID_PRODUCT.matcher(product).matches()) {
            throw new IllegalArgumentException("Invalid product name: " + product);
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + product + ".json").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);

        try {
            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new PriceLookupException("Unknown product: " + product);
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new PriceLookupException(
                        "Price lookup for " + product + " failed with HTTP status " + status);
            }

            PriceResponse response;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                response = gson.fromJson(reader, PriceResponse.class);
            }

            // Fail here rather than letting a bad price surface later in the cart
            if (response == null || response.getPrice() == null) {
                throw new PriceLookupException("Price missing in response for product: " + product);
            }
            if (response.getPrice().signum() < 0) {
                throw new PriceLookupException("Negative price in response for product: " + product);
            }
            return response.getPrice();
        } finally {
            conn.disconnect();
        }
    }
}
