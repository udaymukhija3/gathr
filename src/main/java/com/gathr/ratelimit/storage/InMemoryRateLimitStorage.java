package com.gathr.ratelimit.storage;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of RateLimitStorage using ConcurrentHashMap.
 * Suitable for single-instance deployments.
 * For multi-instance deployments, use RedisRateLimitStorage.
 */
@Component
public class InMemoryRateLimitStorage implements RateLimitStorage {

    private final Map<String, List<Instant>> storage = new ConcurrentHashMap<>();

    @Override
    public void recordRequest(String key, Instant timestamp) {
        storage.compute(key, (k, timestamps) -> {
            if (timestamps == null) {
                timestamps = Collections.synchronizedList(new ArrayList<>());
            }
            timestamps.add(timestamp);
            return timestamps;
        });
    }

    @Override
    public List<Instant> getRequestsInWindow(String key, Instant windowStart) {
        List<Instant> timestamps = storage.get(key);
        if (timestamps == null) {
            return Collections.emptyList();
        }

        synchronized (timestamps) {
            return timestamps.stream()
                    .filter(ts -> !ts.isBefore(windowStart))
                    .toList();
        }
    }

    @Override
    public void cleanup(String key, Instant windowStart) {
        storage.computeIfPresent(key, (k, timestamps) -> {
            synchronized (timestamps) {
                timestamps.removeIf(ts -> ts.isBefore(windowStart));
            }
            return timestamps.isEmpty() ? null : timestamps;
        });
    }

    @Override
    public void clear(String key) {
        storage.remove(key);
    }

    /**
     * Clear all storage (useful for testing).
     */
    public void clearAll() {
        storage.clear();
    }
}
