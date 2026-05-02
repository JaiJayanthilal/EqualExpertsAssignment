package org.cache;

import org.junit.jupiter.api.Test;
import org.service.PriceProvider;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class CaffeineCacheTest {

    @Test
    void shouldLoadFromServiceWhenCacheMiss() throws Exception {
        CaffeineCache cache = new CaffeineCache();
        PriceProvider service = mock(PriceProvider.class);

        when(service.getPrice("cornflakes")).thenReturn(BigDecimal.TEN);

        BigDecimal result = cache.getOrLoad("cornflakes", service);

        assertEquals(BigDecimal.TEN, result);
        verify(service, times(1)).getPrice("cornflakes");
    }

    @Test
    void shouldReturnFromCacheOnSecondCall() throws Exception {
        CaffeineCache cache = new CaffeineCache();
        PriceProvider service = mock(PriceProvider.class);

        when(service.getPrice("cornflakes")).thenReturn(BigDecimal.TEN);

        cache.getOrLoad("cornflakes", service);
        cache.getOrLoad("cornflakes", service);

        verify(service, times(1)).getPrice("cornflakes"); // only once
    }

    @Test
    void shouldWrapException() throws Exception {
        CaffeineCache cache = new CaffeineCache();
        PriceProvider service = mock(PriceProvider.class);

        when(service.getPrice("cornflakes")).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class,
                () -> cache.getOrLoad("cornflakes", service));
    }
}
