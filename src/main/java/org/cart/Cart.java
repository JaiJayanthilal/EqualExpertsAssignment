package org.cart;

import org.model.CartItem;
import org.service.PriceProviderService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Cart {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.125");

    // LinkedHashMap keeps items in the order they were first added
    private final Map<String, CartItem> items = new LinkedHashMap<>();
    private final PriceProviderService priceProvider;

    // Kept up to date on every add, so totals don't re-sum the items each time
    private BigDecimal unroundedSubtotal = BigDecimal.ZERO;

    public Cart(PriceProviderService priceProvider) {
        this.priceProvider = Objects.requireNonNull(priceProvider, "priceProvider");
    }

    public void addProduct(String product, int quantity) throws IOException {
        // Validate before calling the price API, so bad input never costs a request
        if (product == null || product.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive: " + quantity);
        }

        // The price is looked up on the first add and reused for later adds of the same product
        CartItem existing = items.get(product);
        if (existing != null) {
            existing.addQuantity(quantity);
            addToSubtotal(existing.getPrice(), quantity);
            return;
        }

        BigDecimal price = priceProvider.getPrice(product);
        if (price == null) {
            throw new IllegalStateException("No price returned for product: " + product);
        }
        items.put(product, new CartItem(product, quantity, price));
        addToSubtotal(price, quantity);
    }

    private void addToSubtotal(BigDecimal price, int quantity) {
        unroundedSubtotal = unroundedSubtotal.add(price.multiply(BigDecimal.valueOf(quantity)));
    }

    public List<CartItem> getItems() {
        return List.copyOf(items.values());
    }

    // The spec says "Totals should be rounded up where required", so round UP (not HALF_UP)
    public BigDecimal getSubtotal() {
        return unroundedSubtotal.setScale(2, RoundingMode.UP);
    }

    public BigDecimal getTax() {
        return taxOn(getSubtotal());
    }

    public BigDecimal getTotal() {
        BigDecimal subtotal = getSubtotal();
        return subtotal.add(taxOn(subtotal));
    }

    private static BigDecimal taxOn(BigDecimal subtotal) {
        return subtotal
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.UP);
    }
}
