package org.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    @Test
    void shouldCalculateTotalPrice() {
        CartItem item = new CartItem("apple", 2, BigDecimal.TEN);

        assertEquals(new BigDecimal("20"), item.getTotalPrice());
    }

    @Test
    void shouldAddQuantity() {
        CartItem item = new CartItem("apple", 1, BigDecimal.TEN);

        item.addQuantity(2);

        assertEquals(3, item.getQuantity());
    }

    @Test
    void shouldRejectInvalidConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(null, 1, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> new CartItem("  ", 1, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> new CartItem("apple", 0, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> new CartItem("apple", -1, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> new CartItem("apple", 1, null));
        assertThrows(IllegalArgumentException.class, () -> new CartItem("apple", 1, new BigDecimal("-0.01")));
    }

    @Test
    void shouldRejectNonPositiveAddedQuantity() {
        CartItem item = new CartItem("apple", 1, BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class, () -> item.addQuantity(0));
        assertThrows(IllegalArgumentException.class, () -> item.addQuantity(-2));
        assertEquals(1, item.getQuantity());
    }

    @Test
    void shouldRejectQuantityOverflow() {
        CartItem item = new CartItem("apple", Integer.MAX_VALUE, BigDecimal.TEN);

        assertThrows(ArithmeticException.class, () -> item.addQuantity(1));
    }
}
