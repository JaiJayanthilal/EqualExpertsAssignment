package org.service;

/**
 * The price API answered, but not with a usable price
 * (unknown product, error status, or a missing or invalid price).
 */
public class PriceLookupException extends RuntimeException {

    public PriceLookupException(String message) {
        super(message);
    }
}
