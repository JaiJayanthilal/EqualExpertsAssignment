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

    @Test
    void shouldMatchSampleFromSpec() throws Exception {
        PriceProvider provider = mock(PriceProvider.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("2.52"));
        when(provider.getPrice("weetabix")).thenReturn(new BigDecimal("9.98"));

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 1);
        cart.addProduct("weetabix", 1);

        assertEquals(new BigDecimal("15.02"), cart.getSubtotal());
        assertEquals(new BigDecimal("1.88"), cart.getTax());   // 1.8775 → 1.88
        assertEquals(new BigDecimal("16.90"), cart.getTotal());
    }

    @Test
    void shouldRoundTaxUp() throws Exception {
        PriceProvider provider = mock(PriceProvider.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("0.10"));

        Cart cart = new Cart(provider);
        cart.addProduct("cornflakes", 1);

        // 0.10 × 0.125 = 0.0125 → HALF_UP would give 0.01, rounding up gives 0.02
        assertEquals(new BigDecimal("0.02"), cart.getTax());
        assertEquals(new BigDecimal("0.12"), cart.getTotal());
    }

    @Test
    void shouldRoundSubtotalUpWhenPricesHaveMoreThanTwoDecimals() throws Exception {
        PriceProvider provider = mock(PriceProvider.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("1.001"));

        Cart cart = new Cart(provider);
        cart.addProduct("cornflakes", 1);

        assertEquals(new BigDecimal("1.01"), cart.getSubtotal());
    }

    @Test
    void shouldRejectInvalidInputWithoutCallingProvider() throws Exception {
        PriceProvider provider = mock(PriceProvider.class);
        Cart cart = new Cart(provider);

        assertThrows(IllegalArgumentException.class, () -> cart.addProduct(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("", 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("   ", 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("cornflakes", 0));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("cornflakes", -1));

        verifyNoInteractions(provider);
        assertEquals(new BigDecimal("0.00"), cart.getSubtotal());
    }

    @Test
    void shouldRejectNullPriceFromProvider() throws Exception {
        PriceProvider provider = mock(PriceProvider.class); // returns null by default
        Cart cart = new Cart(provider);

        assertThrows(IllegalStateException.class, () -> cart.addProduct("cornflakes", 1));
        assertEquals(new BigDecimal("0.00"), cart.getSubtotal());
    }
}
