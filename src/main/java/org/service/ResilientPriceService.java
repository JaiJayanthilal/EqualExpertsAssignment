package org.service;

import com.google.gson.Gson;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.model.PriceResponse;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.function.Supplier;

public class ResilientPriceService implements PriceProvider {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Gson gson = new Gson();
    private static final String BASE_URL =
            "https://equalexperts.github.io/backend-take-home-test-data/";

    public ResilientPriceService(CircuitBreaker cb, Retry retry) {
        this.circuitBreaker = cb;
        this.retry = retry;
    }

    public BigDecimal getPrice(String product) {
        System.out.println("resilient price provider");

        Supplier<BigDecimal> supplier = () -> fetchPrice(product);

        // Apply retry first, then circuit breaker
        Supplier<BigDecimal> decoratedSupplier =
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        Retry.decorateSupplier(retry, supplier)
                );

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch price", e);
        }
    }

    private BigDecimal fetchPrice(String product) {
        try {
            String urlStr = BASE_URL + product + ".json";

            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            PriceResponse response = gson.fromJson(reader, PriceResponse.class);

            reader.close();

            return response.getPrice();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
