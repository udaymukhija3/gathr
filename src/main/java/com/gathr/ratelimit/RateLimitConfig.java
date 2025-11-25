package com.gathr.ratelimit;

/**
 * Immutable value object representing rate limit configuration.
 * Following the Value Object pattern for thread-safety and immutability.
 */
public record RateLimitConfig(
        String bucket,
        int maxRequests,
        int windowMinutes
) {
    public RateLimitConfig {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or blank");
        }
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("Max requests must be positive");
        }
        if (windowMinutes <= 0) {
            throw new IllegalArgumentException("Window minutes must be positive");
        }
    }

    /**
     * Factory method for common rate limit configurations.
     */
    public static RateLimitConfig perHour(String bucket, int maxRequests) {
        return new RateLimitConfig(bucket, maxRequests, 60);
    }

    public static RateLimitConfig perMinute(String bucket, int maxRequests) {
        return new RateLimitConfig(bucket, maxRequests, 1);
    }

    public static RateLimitConfig perDay(String bucket, int maxRequests) {
        return new RateLimitConfig(bucket, maxRequests, 1440);
    }
}
