package org.cache;

import org.service.SimplePriceService;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentLRUCache {

    private final int capacity;

    private final ConcurrentHashMap<String, BigDecimal> cache;
    private final ConcurrentLinkedDeque<String> order;

    private final ReentrantLock lock = new ReentrantLock();

    public ConcurrentLRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        this.order = new ConcurrentLinkedDeque<>();
    }

    public BigDecimal get(String key) {
        BigDecimal value = cache.get(key);

        if (value != null) {
            // update LRU order
            lock.lock();
            try {
                order.remove(key);
                order.addLast(key);
            } finally {
                lock.unlock();
            }
        }

        return value;
    }

    public void put(String key, BigDecimal value) {
        lock.lock();
        try {
            putInternal(key, value);
        } finally {
            lock.unlock();
        }
    }

    private void putInternal(String key, BigDecimal value) {
        if (cache.containsKey(key)) {
            order.remove(key);
        }

        cache.put(key, value);
        order.addLast(key);

        if (cache.size() > capacity) {
            String lruKey = order.pollFirst();
            if (lruKey != null) {
                cache.remove(lruKey);
            }
        }
    }


    public BigDecimal getOrLoad(String key, SimplePriceService service) throws Exception {
        BigDecimal value = get(key);

        if (value != null) {
            return value;
        }

        lock.lock();
        try {
            // double check (important!)
            value = cache.get(key);
            if (value == null) {
                value = service.getPrice(key);
                put(key, value);

                if (cache.size() > capacity) {
                    String lruKey = order.pollFirst();
                    if (lruKey != null) {
                        cache.remove(lruKey);
                    }
                }
            }

            return value;

        } finally {
            lock.unlock();
        }
    }
}
