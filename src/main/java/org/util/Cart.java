package org.util;

import org.cache.CaffeineCache;
import org.cache.ConcurrentLRUCache;
import org.cache.LRUCache;
import org.model.CartItem;
import org.service.PriceProvider;
import org.service.SimplePriceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class Cart {

    private Map<String, CartItem> items = new HashMap<>();
    private final PriceProvider priceProvider;
    private SimplePriceService simplePriceService = new SimplePriceService();

    //private Map<String, BigDecimal> priceCache = new HashMap<>();
    private Map<String, BigDecimal> lruCache = new LRUCache(3); // Fast but not thread safe
    private final ConcurrentLRUCache concurrentLRUCache = new ConcurrentLRUCache(5); // Manually created cache, Fast and thread safe but error prone
    private final CaffeineCache caffeineCache = new CaffeineCache(); // Cafffeine Library used, to have a thread safe and concurrent cache, tried and tested in production

    public Cart(PriceProvider priceProvider) {
        this.priceProvider = priceProvider;
    }

    private BigDecimal getPrice(String product) throws Exception {
        if (!lruCache.containsKey(product)) {
            lruCache.put(product, simplePriceService.getPrice(product));
        }
        return lruCache.get(product);
    }

    public void addProduct(String product, int quantity) throws Exception {
        //BigDecimal price = getPrice(product);  // this line calls LRU Cache
       // BigDecimal price = concurrentLRUCache.getOrLoad(product, priceService); // this line calls concurrentLRUCache
        BigDecimal price = caffeineCache.getOrLoad(product, priceProvider);  // this line calls caffeine Cache

        if (items.containsKey(product)) {
            items.get(product).addQuantity(quantity);
        } else {
            items.put(product, new CartItem(product, quantity, price));
        }
    }

    public BigDecimal getSubtotal() {
        return items.values().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTax() {
        return getSubtotal()
                .multiply(new BigDecimal("0.125"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotal() {
        return getSubtotal()
                .add(getTax())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void printCart() {
        for (CartItem item : items.values()) {
            System.out.println("Cart contains " +
                    item.getQuantity() + " x " + item.getProduct());
        }

        System.out.println("Subtotal = " + getSubtotal());
        System.out.println("Tax = " + getTax());
        System.out.println("Total = " + getTotal());
    }
}
