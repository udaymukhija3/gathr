package com.gathr.ratelimit;

/**
 * Interface for rate limiting implementations.
 * Follows Dependency Inversion Principle - depend on abstraction, not concretion.
 */
public interface RateLimiter {

    /**
     * Check if a request is allowed and record it if so.
     *
     * @param identifier Unique identifier (userId, IP, phone, etc.)
     * @param config Rate limit configuration
     * @return Result containing allowed status and metadata for headers
     */
    RateLimitResult checkAndRecord(String identifier, RateLimitConfig config);

    /**
     * Check if a request would be allowed without recording it.
     * Useful for read-only checks.
     *
     * @param identifier Unique identifier
     * @param config Rate limit configuration
     * @return Result containing allowed status and metadata
     */
    RateLimitResult check(String identifier, RateLimitConfig config);
}
