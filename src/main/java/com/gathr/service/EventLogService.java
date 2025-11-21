package com.gathr.service;

import com.gathr.entity.Event;
import com.gathr.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EventLogService {

    private static final Logger logger = LoggerFactory.getLogger(EventLogService.class);

    private final EventRepository eventRepository;

    public EventLogService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void log(Long userId, Long activityId, String eventType, Map<String, Object> properties) {
        try {
            Event event = new Event();
            event.setUserId(userId);
            event.setActivityId(activityId);
            event.setEventType(eventType);
            event.setProperties(properties);
            event.setTs(LocalDateTime.now());

            eventRepository.save(event);
            logger.debug("Logged event: type={}, userId={}, activityId={}", eventType, userId, activityId);
        } catch (Exception e) {
            logger.error("Failed to log event: type={}, userId={}, activityId={}", eventType, userId, activityId, e);
            // Don't throw - event logging should not break the main flow
        }
    }

    @Transactional
    public void log(Long userId, String eventType, Map<String, Object> properties) {
        log(userId, null, eventType, properties);
    }

    @Transactional
    public void log(String eventType, Map<String, Object> properties) {
        log(null, null, eventType, properties);
    }
}

