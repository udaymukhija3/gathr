package com.gathr.controller;

import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.EventLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventLogService eventLogService;
    private final AuthenticatedUserService authenticatedUserService;

    public EventController(EventLogService eventLogService, AuthenticatedUserService authenticatedUserService) {
        this.eventLogService = eventLogService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping
    public ResponseEntity<Void> logEvent(
            @RequestBody EventRequest request,
            Authentication authentication) {

        Long userId = authenticatedUserService.extractUserId(authentication).orElse(null);

        try {
            if (request.activityId != null) {
                eventLogService.log(userId, request.activityId, request.eventType, request.properties);
            } else if (userId != null) {
                eventLogService.log(userId, request.eventType, request.properties);
            } else {
                eventLogService.log(request.eventType, request.properties);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Silent failure for telemetry - don't disrupt user flow
            System.err.println("Failed to log event: " + request.eventType + " - " + e.getMessage());
            return ResponseEntity.ok().build(); // Still return 200 to not break frontend
        }
    }

    public record EventRequest(
        String eventType,
        Long activityId,
        Map<String, Object> properties
    ) {}
}
