package com.gathr.service;

import com.gathr.entity.Hub;
import com.gathr.entity.NotificationPreference;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.HubRepository;
import com.gathr.repository.NotificationPreferenceRepository;
import com.gathr.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Service for user onboarding flow.
 * Handles profile completion, interest selection, and location-based hub assignment.
 */
@Service
public class OnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingService.class);
    private static final double MAX_HUB_DISTANCE_KM = 50.0; // Max distance to consider a hub

    private final UserRepository userRepository;
    private final HubRepository hubRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final EventLogService eventLogService;

    public OnboardingService(
            UserRepository userRepository,
            HubRepository hubRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            EventLogService eventLogService) {
        this.userRepository = userRepository;
        this.hubRepository = hubRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.eventLogService = eventLogService;
    }

    /**
     * Complete user onboarding with profile info and interests.
     */
    @Transactional
    public User completeOnboarding(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Update profile
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }

        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        // Set interests (required for onboarding)
        if (request.interests() != null && request.interests().length > 0) {
            user.setInterests(request.interests());
        }

        // Update location if provided
        if (request.latitude() != null && request.longitude() != null) {
            user.updateLocation(request.latitude(), request.longitude());

            // Auto-assign nearest hub if not already set
            if (user.getHomeHub() == null) {
                findAndAssignNearestHub(user, request.latitude(), request.longitude());
            }
        }

        // Mark onboarding complete
        user.setOnboardingCompleted(true);

        user = userRepository.save(user);

        // Create default notification preferences with user's interests
        createDefaultNotificationPreferences(user);

        // Log event
        eventLogService.log(userId, "onboarding_completed", java.util.Map.of(
                "interests", request.interests() != null ? List.of(request.interests()) : List.of(),
                "hasLocation", request.latitude() != null,
                "homeHubId", user.getHomeHub() != null ? user.getHomeHub().getId() : null
        ));

        logger.info("User {} completed onboarding", userId);

        return user;
    }

    /**
     * Update user's location and optionally reassign hub.
     */
    @Transactional
    public User updateLocation(Long userId, BigDecimal latitude, BigDecimal longitude, boolean reassignHub) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.updateLocation(latitude, longitude);

        if (reassignHub) {
            findAndAssignNearestHub(user, latitude, longitude);
        }

        user = userRepository.save(user);

        eventLogService.log(userId, "location_updated", java.util.Map.of(
                "latitude", latitude,
                "longitude", longitude,
                "homeHubId", user.getHomeHub() != null ? user.getHomeHub().getId() : null
        ));

        return user;
    }

    /**
     * Find and assign the nearest hub to user based on coordinates.
     */
    private void findAndAssignNearestHub(User user, BigDecimal latitude, BigDecimal longitude) {
        List<Hub> allHubs = hubRepository.findAll();

        Hub nearestHub = allHubs.stream()
                .filter(hub -> hub.getLatitude() != null && hub.getLongitude() != null)
                .min(Comparator.comparingDouble(hub -> hub.distanceTo(latitude, longitude)))
                .orElse(null);

        if (nearestHub != null) {
            double distance = nearestHub.distanceTo(latitude, longitude);
            if (distance <= MAX_HUB_DISTANCE_KM) {
                user.setHomeHub(nearestHub);
                logger.info("Assigned hub {} to user {} (distance: {:.2f} km)",
                        nearestHub.getName(), user.getId(), distance);
            } else {
                logger.info("No hub within {} km for user {} (nearest: {} at {:.2f} km)",
                        MAX_HUB_DISTANCE_KM, user.getId(), nearestHub.getName(), distance);
            }
        }
    }

    /**
     * Get nearest hubs to given coordinates.
     */
    @Transactional(readOnly = true)
    public List<HubWithDistance> findNearestHubs(BigDecimal latitude, BigDecimal longitude, int limit) {
        List<Hub> allHubs = hubRepository.findAll();

        return allHubs.stream()
                .filter(hub -> hub.getLatitude() != null && hub.getLongitude() != null)
                .map(hub -> new HubWithDistance(hub, hub.distanceTo(latitude, longitude)))
                .filter(hwd -> hwd.distanceKm() <= MAX_HUB_DISTANCE_KM)
                .sorted(Comparator.comparingDouble(HubWithDistance::distanceKm))
                .limit(limit)
                .toList();
    }

    /**
     * Set user's home hub manually.
     */
    @Transactional
    public User setHomeHub(Long userId, Long hubId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new ResourceNotFoundException("Hub", hubId));

        user.setHomeHub(hub);
        user = userRepository.save(user);

        eventLogService.log(userId, "home_hub_set", java.util.Map.of("hubId", hubId));

        return user;
    }

    /**
     * Update user's interests.
     */
    @Transactional
    public User updateInterests(Long userId, String[] interests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setInterests(interests);
        user = userRepository.save(user);

        // Also update notification preferences
        notificationPreferenceRepository.findByUserId(userId).ifPresent(prefs -> {
            prefs.setInterestedCategories(interests);
            notificationPreferenceRepository.save(prefs);
        });

        eventLogService.log(userId, "interests_updated", java.util.Map.of(
                "interests", List.of(interests)
        ));

        return user;
    }

    /**
     * Create default notification preferences for user.
     */
    private void createDefaultNotificationPreferences(User user) {
        if (notificationPreferenceRepository.findByUserId(user.getId()).isEmpty()) {
            NotificationPreference prefs = new NotificationPreference();
            prefs.setUser(user);
            prefs.setPushEnabled(true);
            prefs.setActivityReminders(true);
            prefs.setChatMessages(true);
            prefs.setPromotionalOffers(true);
            prefs.setMaxPromotionsPerDay(3);
            prefs.setInterestedCategories(user.getInterests());

            notificationPreferenceRepository.save(prefs);
        }
    }

    // DTOs

    public record OnboardingRequest(
            String name,
            String bio,
            String avatarUrl,
            String[] interests,
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record HubWithDistance(
            Hub hub,
            double distanceKm
    ) {}
}
