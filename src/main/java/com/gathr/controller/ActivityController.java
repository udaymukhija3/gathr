package com.gathr.controller;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.CreateActivityRequest;
import com.gathr.entity.InviteToken;
import com.gathr.entity.Participation;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.ActivityService;
import com.gathr.service.InviteTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final InviteTokenService inviteTokenService;
    private final AuthenticatedUserService authenticatedUserService;

    public ActivityController(ActivityService activityService, InviteTokenService inviteTokenService, AuthenticatedUserService authenticatedUserService) {
        this.activityService = activityService;
        this.inviteTokenService = inviteTokenService;
        this.authenticatedUserService = authenticatedUserService;
    }
    
    @GetMapping
    public ResponseEntity<?> getActivities(
            @RequestParam(required = false) Long hubId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "5") Double radiusKm) {
        if (hubId != null) {
            List<ActivityDto> activities = activityService.getActivitiesByHub(hubId);
            return ResponseEntity.ok(activities);
        }
        if (latitude != null && longitude != null) {
            double radius = radiusKm != null ? radiusKm : 5d;
            List<ActivityDto> activities = activityService.getActivitiesNearby(latitude, longitude, radius);
            return ResponseEntity.ok(activities);
        }
        return ResponseEntity.badRequest().body(new ErrorResponse("Provide either hubId or latitude/longitude parameters"));
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
        Long userId = authenticatedUserService.requireUserId(authentication);
        ActivityDto activity = activityService.createActivity(request, userId);
        return ResponseEntity.ok(activity);
    }
    
    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "INTERESTED") String status,
            @RequestParam(required = false) String inviteToken,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        Participation.ParticipationStatus participationStatus;
        try {
            participationStatus = Participation.ParticipationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid status. Use INTERESTED or CONFIRMED"));
        }

        activityService.joinActivity(id, userId, participationStatus, inviteToken);
        return ResponseEntity.ok().body(new MessageResponse("Successfully joined activity"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmActivity(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        activityService.confirmActivity(id, userId);
        return ResponseEntity.ok().body(new MessageResponse("Successfully confirmed attendance"));
    }

    @PostMapping("/{id}/invite-token")
    public ResponseEntity<?> generateInviteToken(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        InviteToken token = inviteTokenService.generateInviteToken(id, userId);
        return ResponseEntity.ok().body(Map.of(
            "token", token.getToken(),
            "expiresAt", token.getExpiresAt()
        ));
    }
    
    private record MessageResponse(String message) {}
    private record ErrorResponse(String error) {}
}

