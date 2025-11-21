package com.gathr.controller;

import com.gathr.entity.Event;
import com.gathr.repository.EventRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final EventRepository eventRepository;

    public AnalyticsController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventDto>> getEvents(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            Authentication authentication) {

        Long authenticatedUserId = (Long) authentication.getPrincipal();

        List<Event> events;

        if (userId != null && eventType != null) {
            events = eventRepository.findByUserIdAndEventType(userId, eventType);
        } else if (activityId != null && eventType != null) {
            events = eventRepository.findByActivityIdAndEventType(activityId, eventType);
        } else if (userId != null) {
            events = eventRepository.findByUserId(userId);
        } else if (activityId != null) {
            events = eventRepository.findByActivityId(activityId);
        } else if (eventType != null) {
            events = eventRepository.findByEventType(eventType);
        } else if (since != null) {
            events = eventRepository.findRecentEvents(since);
        } else {
            events = eventRepository.findRecentEvents(LocalDateTime.now().minusDays(7));
        }

        List<EventDto> eventDtos = events.stream()
                .map(event -> new EventDto(
                    event.getId(),
                    event.getUserId(),
                    event.getActivityId(),
                    event.getEventType(),
                    event.getProperties(),
                    event.getTs()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(eventDtos);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        LocalDateTime sinceDate = since != null ? since : LocalDateTime.now().minusDays(7);
        List<Event> events = eventRepository.findRecentEvents(sinceDate);

        // Count events by type
        Map<String, Long> eventCounts = events.stream()
                .collect(Collectors.groupingBy(Event::getEventType, Collectors.counting()));

        // Count unique users
        long uniqueUsers = events.stream()
                .map(Event::getUserId)
                .filter(userId -> userId != null)
                .distinct()
                .count();

        // Count unique activities
        long uniqueActivities = events.stream()
                .map(Event::getActivityId)
                .filter(activityId -> activityId != null)
                .distinct()
                .count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEvents", events.size());
        summary.put("uniqueUsers", uniqueUsers);
        summary.put("uniqueActivities", uniqueActivities);
        summary.put("eventCounts", eventCounts);
        summary.put("since", sinceDate);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/funnel/activity-creation")
    public ResponseEntity<Map<String, Object>> getActivityCreationFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        LocalDateTime sinceDate = since != null ? since : LocalDateTime.now().minusDays(7);
        List<Event> events = eventRepository.findRecentEvents(sinceDate);

        long templateViewed = events.stream()
                .filter(e -> "template_selection_viewed".equals(e.getEventType()))
                .count();

        long creationViewed = events.stream()
                .filter(e -> "activity_creation_viewed".equals(e.getEventType()))
                .count();

        long creationSubmitted = events.stream()
                .filter(e -> "activity_creation_submitted".equals(e.getEventType()))
                .count();

        long creationFailed = events.stream()
                .filter(e -> "activity_creation_failed".equals(e.getEventType()))
                .count();

        Map<String, Object> funnel = new HashMap<>();
        funnel.put("templateViewed", templateViewed);
        funnel.put("creationViewed", creationViewed);
        funnel.put("creationSubmitted", creationSubmitted);
        funnel.put("creationFailed", creationFailed);
        funnel.put("conversionRate", creationViewed > 0 ? (double) creationSubmitted / creationViewed * 100 : 0);

        return ResponseEntity.ok(funnel);
    }

    @GetMapping("/funnel/activity-join")
    public ResponseEntity<Map<String, Object>> getActivityJoinFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {

        LocalDateTime sinceDate = since != null ? since : LocalDateTime.now().minusDays(7);
        List<Event> events = eventRepository.findRecentEvents(sinceDate);

        long feedViewed = events.stream()
                .filter(e -> "feed_viewed".equals(e.getEventType()))
                .count();

        long activityDetailViewed = events.stream()
                .filter(e -> "activity_detail_viewed".equals(e.getEventType()))
                .count();

        long activityJoined = events.stream()
                .filter(e -> "activity_joined".equals(e.getEventType()))
                .count();

        long activityConfirmed = events.stream()
                .filter(e -> "activity_confirmed".equals(e.getEventType()))
                .count();

        long chatJoined = events.stream()
                .filter(e -> "chat_joined".equals(e.getEventType()))
                .count();

        Map<String, Object> funnel = new HashMap<>();
        funnel.put("feedViewed", feedViewed);
        funnel.put("activityDetailViewed", activityDetailViewed);
        funnel.put("activityJoined", activityJoined);
        funnel.put("activityConfirmed", activityConfirmed);
        funnel.put("chatJoined", chatJoined);
        funnel.put("joinConversionRate", activityDetailViewed > 0 ? (double) activityJoined / activityDetailViewed * 100 : 0);
        funnel.put("confirmConversionRate", activityJoined > 0 ? (double) activityConfirmed / activityJoined * 100 : 0);

        return ResponseEntity.ok(funnel);
    }

    public record EventDto(
        Long id,
        Long userId,
        Long activityId,
        String eventType,
        Map<String, Object> properties,
        LocalDateTime ts
    ) {}
}
