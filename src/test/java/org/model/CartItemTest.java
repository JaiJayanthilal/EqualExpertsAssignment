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
}
