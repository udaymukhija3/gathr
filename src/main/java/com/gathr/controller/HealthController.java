package com.gathr.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final BuildProperties buildProperties;

    public HealthController(JdbcTemplate jdbcTemplate, BuildProperties buildProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.buildProperties = buildProperties;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", buildProperties.getVersion());
        health.put("name", buildProperties.getName());

        return ResponseEntity.ok(health);
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();

        try {
            // Test database connection
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbHealth.put("status", "UP");
            dbHealth.put("message", "Database connection is healthy");

            // Get database stats
            try {
                Integer activityCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM activities", Integer.class);
                Integer userCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users", Integer.class);
                Integer eventCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM events", Integer.class);

                Map<String, Integer> stats = new HashMap<>();
                stats.put("activities", activityCount);
                stats.put("users", userCount);
                stats.put("events", eventCount);
                dbHealth.put("stats", stats);
            } catch (Exception e) {
                dbHealth.put("statsError", "Could not retrieve stats: " + e.getMessage());
            }

        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            return ResponseEntity.status(503).body(dbHealth);
        }

        return ResponseEntity.ok(dbHealth);
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();
        boolean isReady = true;
        Map<String, String> checks = new HashMap<>();

        // Check database
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            checks.put("database", "UP");
        } catch (Exception e) {
            checks.put("database", "DOWN: " + e.getMessage());
            isReady = false;
        }

        readiness.put("ready", isReady);
        readiness.put("checks", checks);
        readiness.put("timestamp", LocalDateTime.now());

        return isReady ? ResponseEntity.ok(readiness) : ResponseEntity.status(503).body(readiness);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("alive", true);
        liveness.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(liveness);
    }
}
