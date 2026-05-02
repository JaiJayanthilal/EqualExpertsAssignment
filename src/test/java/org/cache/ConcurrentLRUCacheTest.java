package org.cache;

import org.junit.jupiter.api.Test;
import org.service.SimplePriceService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConcurrentLRUCacheTest {

    @Test
    void shouldPutAndGet() {
        ConcurrentLRUCache cache = new ConcurrentLRUCache(2);

        cache.put("cornflakes", BigDecimal.ONE);

        assertEquals(BigDecimal.ONE, cache.get("cornflakes"));
    }

    @Test
    void shouldEvictLRU() {
        ConcurrentLRUCache cache = new ConcurrentLRUCache(2);

        cache.put("cornflakes", BigDecimal.ONE);
        cache.put("cheerios", BigDecimal.TEN);
        cache.put("weetabix", BigDecimal.ZERO); // evicts "cornflakes"

        assertNull(cache.get("cornflakes"));
    }

    @Test
    void shouldLoadFromServiceOnMiss() throws Exception {
        ConcurrentLRUCache cache = new ConcurrentLRUCache(2);
        SimplePriceService service = mock(SimplePriceService.class);

        when(service.getPrice("cornflakes")).thenReturn(BigDecimal.TEN);

        BigDecimal value = cache.getOrLoad("cornflakes", service);

        assertEquals(BigDecimal.TEN, value);
        verify(service, times(1)).getPrice("cornflakes");
    }

    @Test
    void shouldNotCallServiceOnCacheHit() throws Exception {
        ConcurrentLRUCache cache = new ConcurrentLRUCache(2);
        SimplePriceService service = mock(SimplePriceService.class);

        when(service.getPrice("cornflakes")).thenReturn(BigDecimal.TEN);

        cache.getOrLoad("cornflakes", service);
        cache.getOrLoad("cornflakes", service);

        verify(service, times(1)).getPrice("cornflakes");
    }
}
