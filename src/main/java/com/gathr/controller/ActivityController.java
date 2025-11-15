package com.gathr.controller;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.CreateActivityRequest;
import com.gathr.entity.Participation;
import com.gathr.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activities")
public class ActivityController {
    
    private final ActivityService activityService;
    
    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }
    
    @GetMapping
    public ResponseEntity<List<ActivityDto>> getActivities(@RequestParam(required = false) Long hubId) {
        if (hubId != null) {
            List<ActivityDto> activities = activityService.getActivitiesByHub(hubId);
            return ResponseEntity.ok(activities);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityDto> getActivityById(@PathVariable Long id) {
        ActivityDto activity = activityService.getActivityById(id);
        return ResponseEntity.ok(activity);
    }

    @PostMapping
    public ResponseEntity<ActivityDto> createActivity(
            @Valid @RequestBody CreateActivityRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ActivityDto activity = activityService.createActivity(request, userId);
        return ResponseEntity.ok(activity);
    }
    
    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "INTERESTED") String status,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Participation.ParticipationStatus participationStatus;
        try {
            participationStatus = Participation.ParticipationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid status. Use INTERESTED or CONFIRMED"));
        }
        
        activityService.joinActivity(id, userId, participationStatus);
        return ResponseEntity.ok().body(new MessageResponse("Successfully joined activity"));
    }
    
    private record MessageResponse(String message) {}
    private record ErrorResponse(String error) {}
}

