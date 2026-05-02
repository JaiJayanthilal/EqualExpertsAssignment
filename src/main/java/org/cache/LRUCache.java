package org.cache;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache extends LinkedHashMap<String, BigDecimal> {

    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true); // true = access-order (LRU)
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, BigDecimal> eldest) {
        return size() > capacity;
    }
}
