package org.service;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Looks up the price of a product. Lets the cart be tested without calling the real API.
 */
public interface PriceProviderService {
    BigDecimal getPrice(String product) throws IOException;
}
