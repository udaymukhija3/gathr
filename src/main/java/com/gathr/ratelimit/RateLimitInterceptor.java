package com.gathr.ratelimit;

import com.gathr.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP Interceptor that applies rate limiting to endpoints.
 * Uses the Decorator pattern - wraps request handling with rate limit checks.
 *
 * Adds standard rate limit headers to responses:
 * - X-RateLimit-Limit: Maximum requests allowed
 * - X-RateLimit-Remaining: Requests remaining in window
 * - X-RateLimit-Reset: Seconds until the window resets
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiter rateLimiter;
    private final JwtUtil jwtUtil;
    private final Map<String, RateLimitConfig> endpointConfigs;

    // Default config for endpoints not explicitly configured
    private final RateLimitConfig defaultConfig;

    public RateLimitInterceptor(RateLimiter rateLimiter, JwtUtil jwtUtil) {
        this.rateLimiter = rateLimiter;
        this.jwtUtil = jwtUtil;
        this.endpointConfigs = new ConcurrentHashMap<>();
        this.defaultConfig = RateLimitConfig.perHour("default", 200);

        initializeEndpointConfigs();
    }

    /**
     * Configure rate limits for specific endpoint patterns.
     * Open/Closed Principle: Add new configs without modifying existing code.
     */
    private void initializeEndpointConfigs() {
        // Auth endpoints (by phone/IP for unauthenticated requests)
        registerConfig("POST:/auth/otp/start", RateLimitConfig.perHour("otp_start", 3));
        registerConfig("POST:/auth/otp/verify", RateLimitConfig.perHour("otp_verify", 10));

        // Activity endpoints (by user)
        registerConfig("POST:/activities", RateLimitConfig.perHour("activity_create", 10));
        registerConfig("POST:/activities/*/join", RateLimitConfig.perHour("activity_join", 30));
        registerConfig("POST:/activities/*/confirm", RateLimitConfig.perHour("activity_confirm", 30));
        registerConfig("POST:/activities/*/invite-token", RateLimitConfig.perHour("invite_token", 20));

        // Message endpoints (higher limit for chat)
        registerConfig("POST:/activities/*/messages", RateLimitConfig.perHour("message_send", 120));
        registerConfig("GET:/activities/*/messages", RateLimitConfig.perHour("message_fetch", 300));

        // Report endpoint (prevent spam reports)
        registerConfig("POST:/reports", RateLimitConfig.perHour("report_create", 10));

        // Contact upload (prevent abuse)
        registerConfig("POST:/contacts/upload", RateLimitConfig.perHour("contacts_upload", 5));

        // Feedback (one per activity realistically)
        registerConfig("POST:/feedbacks", RateLimitConfig.perHour("feedback_create", 20));

        // Template endpoints
        registerConfig("POST:/activity-templates", RateLimitConfig.perHour("template_create", 10));
    }

    public void registerConfig(String pattern, RateLimitConfig config) {
        endpointConfigs.put(pattern, config);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Skip rate limiting for certain paths
        if (shouldSkipRateLimit(path)) {
            return true;
        }

        // Find matching config
        RateLimitConfig config = findConfig(method, path);
        String identifier = extractIdentifier(request);

        // Perform rate limit check
        RateLimitResult result = rateLimiter.checkAndRecord(identifier, config);

        // Always add rate limit headers
        addRateLimitHeaders(response, result);

        if (!result.allowed()) {
            logger.warn("Rate limited: method={}, path={}, identifier={}", method, path, identifier);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.resetInSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please try again in " + result.resetInSeconds() + " seconds.\"}"
            );
            return false;
        }

        return true;
    }

    private boolean shouldSkipRateLimit(String path) {
        // Skip health checks, static resources, WebSocket
        return path.equals("/health") ||
                path.startsWith("/ws") ||
                path.startsWith("/actuator");
    }

    private RateLimitConfig findConfig(String method, String path) {
        // Try exact match first
        String exactKey = method + ":" + path;
        if (endpointConfigs.containsKey(exactKey)) {
            return endpointConfigs.get(exactKey);
        }

        // Try wildcard patterns (e.g., POST:/activities/*/join)
        for (Map.Entry<String, RateLimitConfig> entry : endpointConfigs.entrySet()) {
            if (matchesPattern(entry.getKey(), method, path)) {
                return entry.getValue();
            }
        }

        // Return default config
        return defaultConfig;
    }

    private boolean matchesPattern(String pattern, String method, String path) {
        String[] parts = pattern.split(":", 2);
        if (parts.length != 2) return false;

        String patternMethod = parts[0];
        String patternPath = parts[1];

        if (!patternMethod.equals(method)) return false;

        // Convert wildcard pattern to regex
        String regex = patternPath
                .replace("*", "[^/]+")
                .replace("/", "\\/");

        return path.matches(regex);
    }

    private String extractIdentifier(HttpServletRequest request) {
        // Try to get user ID from JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return "user:" + userId;
                }
            } catch (Exception e) {
                logger.debug("Could not extract user ID from token: {}", e.getMessage());
            }
        }

        // Fall back to IP address for unauthenticated requests
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded headers (behind proxy/load balancer)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // Take first IP in chain
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetInSeconds()));
    }
}
