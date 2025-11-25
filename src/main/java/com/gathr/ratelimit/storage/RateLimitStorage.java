package com.gathr.ratelimit.storage;

import java.time.Instant;
import java.util.List;

/**
 * Strategy interface for rate limit storage.
 * Follows Interface Segregation Principle - only storage operations.
 * Implementations can use in-memory, Redis, database, etc.
 */
public interface RateLimitStorage {

    /**
     * Record a request timestamp for the given key.
     *
     * @param key Unique key combining bucket and identifier
     * @param timestamp When the request occurred
     */
    void recordRequest(String key, Instant timestamp);

    /**
     * Get all request timestamps within the specified window.
     *
     * @param key Unique key combining bucket and identifier
     * @param windowStart Start of the time window
     * @return List of timestamps within the window
     */
    List<Instant> getRequestsInWindow(String key, Instant windowStart);

    /**
     * Remove expired entries older than the window start.
     * Called periodically to prevent memory leaks.
     *
     * @param key Unique key combining bucket and identifier
     * @param windowStart Entries before this time will be removed
     */
    void cleanup(String key, Instant windowStart);

    /**
     * Clear all data for a key (useful for testing).
     */
    void clear(String key);
}
