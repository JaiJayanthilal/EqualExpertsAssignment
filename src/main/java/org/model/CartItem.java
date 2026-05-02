package org.model;

import java.math.BigDecimal;

public class CartItem {
    private String product;
    private int quantity;
    private BigDecimal price;

    public CartItem(String product, int quantity, BigDecimal price) {
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
        this.quantity += qty;
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
