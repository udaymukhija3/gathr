package com.gathr.controller;

import com.gathr.dto.ActivityDto;
import com.gathr.dto.CreateActivityRequest;
import com.gathr.dto.common.ApiResponse;
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
    private final com.gathr.recs.CreationAssistant creationAssistant;
    private final com.gathr.repository.UserRepository userRepository;

    public ActivityController(
            ActivityService activityService,
            InviteTokenService inviteTokenService,
            AuthenticatedUserService authenticatedUserService,
            com.gathr.recs.CreationAssistant creationAssistant,
            com.gathr.repository.UserRepository userRepository) {
        this.activityService = activityService;
        this.inviteTokenService = inviteTokenService;
        this.authenticatedUserService = authenticatedUserService;
        this.creationAssistant = creationAssistant;
        this.userRepository = userRepository;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<com.gathr.dto.ActivitySuggestionDto>>> getSuggestions(
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        com.gathr.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.gathr.exception.ResourceNotFoundException("User", userId));

        return ResponseEntity.ok(ApiResponse.success(creationAssistant.suggestActivities(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityDto>>> getActivities(
            @RequestParam(required = false) Long hubId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "5") Double radiusKm) {
        if (hubId != null) {
            List<ActivityDto> activities = activityService.getActivitiesByHub(hubId);
            return ResponseEntity.ok(ApiResponse.success(activities));
        }
        if (latitude != null && longitude != null) {
            double radius = radiusKm != null ? radiusKm : 5d;
            List<ActivityDto> activities = activityService.getActivitiesNearby(latitude, longitude, radius);
            return ResponseEntity.ok(ApiResponse.success(activities));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Provide either hubId or latitude/longitude parameters", "INVALID_PARAMS"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ActivityDto>> getActivityById(@PathVariable Long id) {
        ActivityDto activity = activityService.getActivityById(id);
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ActivityDto>> createActivity(
            @Valid @RequestBody CreateActivityRequest request,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        ActivityDto activity = activityService.createActivity(request, userId);
        return ResponseEntity.ok(ApiResponse.success(activity, "Activity created successfully"));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinActivity(
            @PathVariable Long id,
            @RequestParam(defaultValue = "INTERESTED") String status,
            @RequestParam(required = false) String inviteToken,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        Participation.ParticipationStatus participationStatus;
        try {
            participationStatus = Participation.ParticipationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status. Use INTERESTED or CONFIRMED", "INVALID_STATUS"));
        }

        activityService.joinActivity(id, userId, participationStatus, inviteToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully joined activity"));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmActivity(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        activityService.confirmActivity(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully confirmed attendance"));
    }

    @PostMapping("/{id}/invite-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateInviteToken(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        InviteToken token = inviteTokenService.generateInviteToken(id, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "token", token.getToken(),
                "expiresAt", token.getExpiresAt())));
    }
}
