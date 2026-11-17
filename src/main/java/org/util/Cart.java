package org.util;


import org.cache.LRUCache;
import org.model.CartItem;
import org.service.PriceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class Cart {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.125");

    private Map<String, CartItem> items = new HashMap<>();
    private final PriceProvider priceProvider;

    //private Map<String, BigDecimal> priceCache = new HashMap<>();
    private Map<String, BigDecimal> lruCache = new LRUCache(3); // Fast but not thread safe

    public Cart(PriceProvider priceProvider) {
        this.priceProvider = priceProvider;
    }

    private BigDecimal getPrice(String product) throws Exception {
        if (!lruCache.containsKey(product)) {
            BigDecimal price = priceProvider.getPrice(product);
            if (price == null) {
                throw new IllegalStateException("No price returned for product: " + product);
            }
            lruCache.put(product, price);
        }
        return lruCache.get(product);
    }

    public void addProduct(String product, int quantity) throws Exception {
        // Validate before calling the price API, so bad input never costs a request
        if (product == null || product.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }

        BigDecimal price = getPrice(product);  // this line calls LRU Cache

        if (items.containsKey(product)) {
            items.get(product).addQuantity(quantity);
        } else {
            items.put(product, new CartItem(product, quantity, price));
        }
    }

    // The spec says "Totals should be rounded up where required", so round UP (not HALF_UP)
    public BigDecimal getSubtotal() {
        return items.values().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UP);
    }

    public BigDecimal getTax() {
        return taxOn(getSubtotal());
    }

    public BigDecimal getTotal() {
        BigDecimal subtotal = getSubtotal();
        return totalOf(subtotal, taxOn(subtotal));
    }

    public void printCart() {
        for (CartItem item : items.values()) {
            System.out.println("Cart contains " +
                    item.getQuantity() + " x " + item.getProduct());
        }

        // Calculate the subtotal once and derive tax and total from it
        BigDecimal subtotal = getSubtotal();
        BigDecimal tax = taxOn(subtotal);

        System.out.println("Subtotal = " + subtotal);
        System.out.println("Tax = " + tax);
        System.out.println("Total = " + totalOf(subtotal, tax));
    }

    private static BigDecimal taxOn(BigDecimal subtotal) {
        return subtotal
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.UP);
    }

    private static BigDecimal totalOf(BigDecimal subtotal, BigDecimal tax) {
        return subtotal
                .add(tax)
                .setScale(2, RoundingMode.UP);
    }
}
