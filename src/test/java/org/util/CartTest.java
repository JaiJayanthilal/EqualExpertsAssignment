package org.util;

import org.junit.jupiter.api.Test;
import org.service.PriceProvider;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartTest {

    @Test
    void shouldAddProductsAndCalculateTotals() throws Exception {
        PriceProvider provider = mock(PriceProvider.class);

        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("2.50"));
        when(provider.getPrice("weetabix")).thenReturn(new BigDecimal("3.00"));

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 2);
        cart.addProduct("weetabix", 1);

        assertEquals(new BigDecimal("8.00"), cart.getSubtotal());
        assertEquals(new BigDecimal("1.00"), cart.getTax());
        assertEquals(new BigDecimal("9.00"), cart.getTotal());
    }

    @Test
    void shouldMergeSameProduct() throws Exception {
        PriceProvider provider = mock(PriceProvider.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("2.50"));

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 2);

        assertEquals(new BigDecimal("7.50"), cart.getSubtotal());
    }
}
