package com.gathr.ratelimit;

/**
 * Immutable result of a rate limit check.
 * Contains all information needed for response headers and decision making.
 */
public record RateLimitResult(
        boolean allowed,
        int remaining,
        long resetInSeconds,
        int limit
) {
    public static RateLimitResult allowed(int remaining, long resetInSeconds, int limit) {
        return new RateLimitResult(true, remaining, resetInSeconds, limit);
    }

    public static RateLimitResult denied(long resetInSeconds, int limit) {
        return new RateLimitResult(false, 0, resetInSeconds, limit);
    }
}
