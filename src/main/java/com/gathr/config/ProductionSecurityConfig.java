package com.gathr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Additional security configuration for production environment
 *
 * Activate with: -Dspring.profiles.active=prod
 *
 * This configuration adds HSTS (HTTP Strict Transport Security) headers
 * which should only be enabled when the application is served over HTTPS.
 */
@Configuration
@Profile("prod")
public class ProductionSecurityConfig {

    /**
     * Additional security headers for production
     *
     * This filter adds:
     * - Strict-Transport-Security (HSTS)
     * - Referrer-Policy
     * - Permissions-Policy
     */
    @Bean
    public OncePerRequestFilter productionSecurityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {

                // HSTS: Force HTTPS for 1 year, including subdomains
                response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");

                // Referrer Policy: Only send origin on cross-origin requests
                response.setHeader("Referrer-Policy",
                    "strict-origin-when-cross-origin");

                // Permissions Policy: Disable unused browser features
                response.setHeader("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=()");

                filterChain.doFilter(request, response);
            }
        };
    }
}
