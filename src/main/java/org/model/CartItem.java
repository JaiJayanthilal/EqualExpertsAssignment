package org.model;

import java.math.BigDecimal;

public class CartItem {
    private final String product;
    private int quantity;
    private final BigDecimal price;

    public CartItem(String product, int quantity, BigDecimal price) {
        if (product == null || product.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must be zero or more: " + price);
        }
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + qty);
        }
        this.quantity = Math.addExact(this.quantity, qty); // throws instead of overflowing
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
