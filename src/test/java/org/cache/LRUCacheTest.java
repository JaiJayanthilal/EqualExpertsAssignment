package org.cache;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {

    @Test
    void shouldEvictOldestEntry() {
        LRUCache cache = new LRUCache(2);

        cache.put("cornflakes", BigDecimal.ONE);
        cache.put("cheerios", BigDecimal.TEN);
        cache.put("weetabix", BigDecimal.ZERO);

        assertFalse(cache.containsKey("cornflakes"));
    }

    @Test
    void shouldUpdateAccessOrder() {
        LRUCache cache = new LRUCache(2);

        cache.put("cornflakes", BigDecimal.ONE);
        cache.put("cheerios", BigDecimal.TEN);

        cache.get("cornflakes"); // makes "cheerios" LRU

        cache.put("weetabix", BigDecimal.ZERO);

        assertFalse(cache.containsKey("cheerios"));
    }
}
