package com.gathr.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for extracting authenticated user information.
 * Centralizes authentication principal extraction to avoid repeated casting
 * and provides a single point of change if authentication mechanism changes.
 */
@Service
public class AuthenticatedUserService {

    /**
     * Get the current authenticated user's ID from the security context.
     *
     * @return Optional containing the user ID if authenticated, empty otherwise
     */
    public Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractUserId(authentication);
    }

    /**
     * Get the current authenticated user's ID, throwing if not authenticated.
     *
     * @return The authenticated user's ID
     * @throws IllegalStateException if no user is authenticated
     */
    public Long requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Extract user ID from an Authentication object.
     *
     * @param authentication The authentication object (can be null)
     * @return Optional containing the user ID if valid, empty otherwise
     */
    public Optional<Long> extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return Optional.of(userId);
        }

        return Optional.empty();
    }

    /**
     * Extract user ID from Authentication, throwing if not present.
     *
     * @param authentication The authentication object
     * @return The user ID
     * @throws IllegalStateException if authentication is invalid or user ID cannot be extracted
     */
    public Long requireUserId(Authentication authentication) {
        return extractUserId(authentication)
                .orElseThrow(() -> new IllegalStateException("Could not extract user ID from authentication"));
    }
}
