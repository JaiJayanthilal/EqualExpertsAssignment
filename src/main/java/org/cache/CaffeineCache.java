package org.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.service.PriceProvider;
import org.service.SimplePriceService;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class CaffeineCache {

    private final Cache<String, BigDecimal> cache;

    public CaffeineCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(100) // capacity
                .expireAfterWrite(10, TimeUnit.MINUTES) // optional
                .build();
    }

    public BigDecimal getOrLoad(String key, PriceProvider service) {
        return cache.get(key, k -> {
            try {
                return service.getPrice(k);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}