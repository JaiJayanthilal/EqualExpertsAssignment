package org.service;

import com.google.gson.Gson;
import org.designprinciple.CircuitBreaker;
import org.model.PriceResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLOutput;

import javax.net.ssl.HttpsURLConnection;

public class SimplePriceService implements PriceProvider {

    private static final String BASE_URL =
            "https://equalexperts.github.io/backend-take-home-test-data/";

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final Gson gson = new Gson();

    // Circuit Breaker instance
    private final CircuitBreaker circuitBreaker =
            new CircuitBreaker(3, 5000); // 3 failures → OPEN, retry after 5 sec

    @Override
    public BigDecimal getPrice(String product) throws Exception {
        System.out.println("simple price provider "+ product);

        if (!circuitBreaker.allowRequest()) {
            throw new RuntimeException("Circuit is OPEN / HALF_OPEN busy. Skipping API call");
        }

        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                attempts++;

                BigDecimal price = fetchPrice(product);

                circuitBreaker.recordSuccess();
                return price;

            } catch (Exception e) {

                circuitBreaker.recordFailure();

                if (attempts >= MAX_RETRIES) {
                    throw new RuntimeException("Failed after retries", e);
                }

                Thread.sleep(RETRY_DELAY_MS);
            }
        }

        throw new RuntimeException("Unexpected error");
    }

    protected BigDecimal fetchPrice(String product) throws Exception {

        String urlStr = BASE_URL + product + ".json";

        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();

        if (status != 200) {
            throw new RuntimeException("Failed with HTTP status: " + status);
        }

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

        PriceResponse response = gson.fromJson(reader, PriceResponse.class);

        reader.close();

        return response.getPrice();
    }
}
