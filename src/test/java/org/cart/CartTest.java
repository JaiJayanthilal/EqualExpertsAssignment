package org.cart;

import org.junit.jupiter.api.Test;
import org.exception.PriceLookupException;
import org.model.CartItem;
import org.service.PriceProviderService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartTest {

    @Test
    void shouldMatchSampleFromSpec() throws Exception {
        PriceProviderService provider = mock(PriceProviderService.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("2.52"));
        when(provider.getPrice("weetabix")).thenReturn(new BigDecimal("9.98"));

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 1);
        cart.addProduct("weetabix", 1);

        List<CartItem> items = cart.getItems();
        assertEquals(2, items.size());
        assertEquals("cornflakes", items.get(0).getProduct());
        assertEquals(2, items.get(0).getQuantity());
        assertEquals("weetabix", items.get(1).getProduct());
        assertEquals(1, items.get(1).getQuantity());

        assertEquals(new BigDecimal("15.02"), cart.getSubtotal());
        assertEquals(new BigDecimal("1.88"), cart.getTax());   // 1.8775 → 1.88
        assertEquals(new BigDecimal("16.90"), cart.getTotal());
    }

    @Test
    void shouldLookUpPriceOnlyOnFirstAdd() throws Exception {
        PriceProviderService provider = mock(PriceProviderService.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("2.50"));

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 2);

        assertEquals(new BigDecimal("7.50"), cart.getSubtotal());
        verify(provider, times(1)).getPrice("cornflakes");
    }

    @Test
    void shouldHaveZeroTotalsWhenEmpty() {
        Cart cart = new Cart(mock(PriceProviderService.class));

        assertTrue(cart.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), cart.getSubtotal());
        assertEquals(new BigDecimal("0.00"), cart.getTax());
        assertEquals(new BigDecimal("0.00"), cart.getTotal());
    }

    @Test
    void shouldRoundTaxUp() throws Exception {
        PriceProviderService provider = mock(PriceProviderService.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("0.10"));

        Cart cart = new Cart(provider);
        cart.addProduct("cornflakes", 1);

        // 0.10 × 0.125 = 0.0125 → HALF_UP would give 0.01, rounding up gives 0.02
        assertEquals(new BigDecimal("0.02"), cart.getTax());
        assertEquals(new BigDecimal("0.12"), cart.getTotal());
    }

    @Test
    void shouldRoundSubtotalUpWhenPricesHaveMoreThanTwoDecimals() throws Exception {
        PriceProviderService provider = mock(PriceProviderService.class);
        when(provider.getPrice("cornflakes")).thenReturn(new BigDecimal("1.001"));

        Cart cart = new Cart(provider);
        cart.addProduct("cornflakes", 1);

        assertEquals(new BigDecimal("1.01"), cart.getSubtotal());
    }

    @Test
    void shouldRejectInvalidInputWithoutCallingProvider() {
        PriceProviderService provider = mock(PriceProviderService.class);
        Cart cart = new Cart(provider);

        assertThrows(IllegalArgumentException.class, () -> cart.addProduct(null, 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("", 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("   ", 1));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("cornflakes", 0));
        assertThrows(IllegalArgumentException.class, () -> cart.addProduct("cornflakes", -1));

        verifyNoInteractions(provider);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void shouldRejectNullPriceFromProvider() {
        PriceProviderService provider = mock(PriceProviderService.class); // returns null by default
        Cart cart = new Cart(provider);

        assertThrows(IllegalStateException.class, () -> cart.addProduct("cornflakes", 1));
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void shouldLeaveCartUnchangedWhenPriceLookupFails() throws Exception {
        PriceProviderService provider = mock(PriceProviderService.class);
        when(provider.getPrice("unknown")).thenThrow(new PriceLookupException("Unknown product: unknown"));

        Cart cart = new Cart(provider);

        assertThrows(PriceLookupException.class, () -> cart.addProduct("unknown", 1));
        assertTrue(cart.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), cart.getTotal());
    }
}
