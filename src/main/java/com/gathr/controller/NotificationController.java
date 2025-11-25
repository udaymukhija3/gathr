package com.gathr.controller;

import com.gathr.entity.Notification;
import com.gathr.entity.NotificationPreference;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.NotificationPreferenceRepository;
import com.gathr.repository.UserRepository;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing notifications and notification preferences.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public NotificationController(
            NotificationService notificationService,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService) {
        this.notificationService = notificationService;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId).stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        int count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
        authenticatedUserService.requireUserId(authentication);
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<PreferencesResponse> getPreferences(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        NotificationPreference prefs = preferenceRepository.findByUserId(userId)
                .orElse(createDefaultPreferences(userId));

        return ResponseEntity.ok(toPreferencesResponse(prefs));
    }

    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        NotificationPreference prefs = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    NotificationPreference newPrefs = new NotificationPreference();
                    newPrefs.setUser(user);
                    return newPrefs;
                });

        if (request.pushEnabled() != null) {
            prefs.setPushEnabled(request.pushEnabled());
        }
        if (request.activityReminders() != null) {
            prefs.setActivityReminders(request.activityReminders());
        }
        if (request.chatMessages() != null) {
            prefs.setChatMessages(request.chatMessages());
        }
        if (request.promotionalOffers() != null) {
            prefs.setPromotionalOffers(request.promotionalOffers());
        }
        if (request.maxPromotionsPerDay() != null) {
            prefs.setMaxPromotionsPerDay(request.maxPromotionsPerDay());
        }
        if (request.quietHoursStart() != null) {
            prefs.setQuietHoursStart(request.quietHoursStart());
        }
        if (request.quietHoursEnd() != null) {
            prefs.setQuietHoursEnd(request.quietHoursEnd());
        }
        if (request.interestedCategories() != null) {
            prefs.setInterestedCategories(request.interestedCategories().toArray(new String[0]));
        }

        prefs = preferenceRepository.save(prefs);

        return ResponseEntity.ok(toPreferencesResponse(prefs));
    }

    private NotificationPreference createDefaultPreferences(Long userId) {
        NotificationPreference prefs = new NotificationPreference();
        prefs.setPushEnabled(true);
        prefs.setActivityReminders(true);
        prefs.setChatMessages(true);
        prefs.setPromotionalOffers(true);
        prefs.setMaxPromotionsPerDay(3);
        return prefs;
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getActivity() != null ? notification.getActivity().getId() : null,
                notification.getPromotion() != null ? notification.getPromotion().getId() : null,
                notification.getCreatedAt().toString(),
                notification.getReadAt() != null
        );
    }

    private PreferencesResponse toPreferencesResponse(NotificationPreference prefs) {
        return new PreferencesResponse(
                prefs.getPushEnabled(),
                prefs.getActivityReminders(),
                prefs.getChatMessages(),
                prefs.getPromotionalOffers(),
                prefs.getMaxPromotionsPerDay(),
                prefs.getQuietHoursStart(),
                prefs.getQuietHoursEnd(),
                prefs.getInterestedCategories() != null ? List.of(prefs.getInterestedCategories()) : List.of()
        );
    }

    public record NotificationDto(
            Long id,
            String type,
            String title,
            String body,
            Long activityId,
            Long promotionId,
            String createdAt,
            boolean isRead
    ) {}

    public record UnreadCountResponse(int count) {}

    public record PreferencesResponse(
            Boolean pushEnabled,
            Boolean activityReminders,
            Boolean chatMessages,
            Boolean promotionalOffers,
            Integer maxPromotionsPerDay,
            Integer quietHoursStart,
            Integer quietHoursEnd,
            List<String> interestedCategories
    ) {}

    public record UpdatePreferencesRequest(
            Boolean pushEnabled,
            Boolean activityReminders,
            Boolean chatMessages,
            Boolean promotionalOffers,
            Integer maxPromotionsPerDay,
            Integer quietHoursStart,
            Integer quietHoursEnd,
            List<String> interestedCategories
    ) {}
}
