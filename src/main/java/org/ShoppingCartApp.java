package org;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.designprinciple.ResilienceConfig;
import org.service.PriceProvider;
import org.service.ResilientPriceService;
import org.service.SimplePriceService;
import org.util.Cart;

public class ShoppingCartApp {

    public static void main(String[] args) throws Exception {
        PriceProvider provider = new SimplePriceService(); // Simple Price Provider, has manual circuit breaker and manual retry logic

        /* Below 3 lines of code uses resilence4j library to implement Circuit breaker and Retry logic
        To test them, comment out the above SimplePriceService, and uncomment the below 3 lines.
        Here I have implemented strategy pattern, using which we can incorporate plug and play of either of the 2 provider(s)
        SimplePriceService or ResilientPriceService

        CircuitBreaker cb = ResilienceConfig.createCircuitBreaker();
        Retry retry = ResilienceConfig.createRetry();
        PriceProvider provider = new ResilientPriceService(cb, retry);
         */

        Cart cart = new Cart(provider);

        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 1);
        cart.addProduct("weetabix", 1);

        cart.printCart();
    }
}
