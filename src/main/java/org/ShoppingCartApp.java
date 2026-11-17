package org;

import org.service.PriceProvider;
import org.service.SimplePriceService;
import org.util.Cart;

public class ShoppingCartApp {

    public static void main(String[] args) throws Exception {
        PriceProvider provider = new SimplePriceService(); // Simple Price Provider, has manual circuit breaker and manual retry logic

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 1);
        cart.addProduct("weetabix", 1);

        cart.printCart();
    }
}
