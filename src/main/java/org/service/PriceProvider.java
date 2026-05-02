package org.service;

import java.math.BigDecimal;

public interface PriceProvider {
    BigDecimal getPrice(String product) throws Exception;
}
