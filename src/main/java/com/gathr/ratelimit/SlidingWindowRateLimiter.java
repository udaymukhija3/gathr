package com.gathr.ratelimit;

import com.gathr.ratelimit.storage.RateLimitStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sliding window rate limiter implementation.
 * Uses the Strategy pattern for storage (can be swapped for Redis).
 *
 * Algorithm: Count requests in a sliding time window.
 * More accurate than fixed window, prevents burst at window boundaries.
 */
@Component
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    private final RateLimitStorage storage;

    public SlidingWindowRateLimiter(RateLimitStorage storage) {
        this.storage = storage;
    }

    @Override
    public RateLimitResult checkAndRecord(String identifier, RateLimitConfig config) {
        String key = buildKey(config.bucket(), identifier);
        Instant now = Instant.now();
        Instant windowStart = now.minus(Duration.ofMinutes(config.windowMinutes()));

        // Cleanup old entries
        storage.cleanup(key, windowStart);

        // Get current count
        List<Instant> requests = storage.getRequestsInWindow(key, windowStart);
        int currentCount = requests.size();

        if (currentCount >= config.maxRequests()) {
            long resetSeconds = calculateResetSeconds(requests, config.windowMinutes(), now);
            logger.warn("Rate limit exceeded: bucket={}, identifier={}, count={}/{}",
                    config.bucket(), identifier, currentCount, config.maxRequests());
            return RateLimitResult.denied(resetSeconds, config.maxRequests());
        }

        // Record this request
        storage.recordRequest(key, now);

        int remaining = config.maxRequests() - currentCount - 1;
        long resetSeconds = calculateResetSeconds(requests, config.windowMinutes(), now);

        logger.debug("Rate limit check passed: bucket={}, identifier={}, remaining={}",
                config.bucket(), identifier, remaining);

        return RateLimitResult.allowed(remaining, resetSeconds, config.maxRequests());
    }

    @Override
    public RateLimitResult check(String identifier, RateLimitConfig config) {
        String key = buildKey(config.bucket(), identifier);
        Instant now = Instant.now();
        Instant windowStart = now.minus(Duration.ofMinutes(config.windowMinutes()));

        List<Instant> requests = storage.getRequestsInWindow(key, windowStart);
        int currentCount = requests.size();

        if (currentCount >= config.maxRequests()) {
            long resetSeconds = calculateResetSeconds(requests, config.windowMinutes(), now);
            return RateLimitResult.denied(resetSeconds, config.maxRequests());
        }

        int remaining = config.maxRequests() - currentCount;
        long resetSeconds = calculateResetSeconds(requests, config.windowMinutes(), now);

        return RateLimitResult.allowed(remaining, resetSeconds, config.maxRequests());
    }

    private String buildKey(String bucket, String identifier) {
        return bucket + ":" + identifier;
    }

    private long calculateResetSeconds(List<Instant> requests, int windowMinutes, Instant now) {
        if (requests.isEmpty()) {
            return Duration.ofMinutes(windowMinutes).toSeconds();
        }

        // Find oldest request in window - that's when first slot opens
        Instant oldest = requests.stream()
                .min(Instant::compareTo)
                .orElse(now);

        Instant resetTime = oldest.plus(Duration.ofMinutes(windowMinutes));
        long seconds = Duration.between(now, resetTime).toSeconds();

        return Math.max(0, seconds);
    }
}
