package com.gathr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@Service
public class EventLogService {

    private static final Logger logger = LoggerFactory.getLogger(EventLogService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EventLogService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Log an event asynchronously to avoid blocking the main thread.
     * Uses direct JDBC for performance and simplicity.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, Long activityId, String eventType, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            String sql = "INSERT INTO event_logs (user_id, event_type, event_time, payload, created_at) VALUES (?, ?, ?, ?::jsonb, ?)";

            jdbcTemplate.update(sql,
                    userId,
                    eventType,
                    Timestamp.from(Instant.now()),
                    payloadJson,
                    Timestamp.from(Instant.now()));

            logger.debug("Logged event: {} for user: {}", eventType, userId);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event payload", e);
        } catch (Exception e) {
            logger.error("Failed to log event: " + eventType, e);
        }
    }

    // Overload for convenience
    public void log(Long userId, String eventType, Map<String, Object> payload) {
        log(userId, null, eventType, payload);
    }

    // Overload for system events (no user)
    public void log(String eventType, Map<String, Object> payload) {
        log(null, null, eventType, payload);
    }
}
