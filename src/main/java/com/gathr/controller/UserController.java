package com.gathr.controller;

import com.gathr.dto.TrustScoreDto;
import com.gathr.dto.common.ApiResponse;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.UserRepository;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.OnboardingService;
import com.gathr.service.TrustScoreService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

        private final TrustScoreService trustScoreService;
        private final OnboardingService onboardingService;
        private final UserRepository userRepository;
        private final AuthenticatedUserService authenticatedUserService;

        public UserController(
                        TrustScoreService trustScoreService,
                        OnboardingService onboardingService,
                        UserRepository userRepository,
                        AuthenticatedUserService authenticatedUserService) {
                this.trustScoreService = trustScoreService;
                this.onboardingService = onboardingService;
                this.userRepository = userRepository;
                this.authenticatedUserService = authenticatedUserService;
        }

        // ==================== PROFILE ====================

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
        }

        @PutMapping("/me")
        public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
                        @Valid @RequestBody UpdateProfileRequest request,
                        Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

                if (request.name() != null && !request.name().isBlank()) {
                        user.setName(request.name().trim());
                }
                if (request.bio() != null) {
                        user.setBio(request.bio().trim());
                }
                if (request.avatarUrl() != null) {
                        user.setAvatarUrl(request.avatarUrl());
                }

                user = userRepository.save(user);
                return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
        }

        // ==================== ONBOARDING ====================

        @PostMapping("/me/onboarding")
        public ResponseEntity<ApiResponse<UserProfileResponse>> completeOnboarding(
                        @Valid @RequestBody OnboardingRequest request,
                        Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);

                User user = onboardingService.completeOnboarding(userId, new OnboardingService.OnboardingRequest(
                                request.name(),
                                request.bio(),
                                request.avatarUrl(),
                                request.interests(),
                                request.latitude(),
                                request.longitude()));

                return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
        }

        @GetMapping("/me/onboarding-status")
        public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getOnboardingStatus(
                        Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

                return ResponseEntity.ok(ApiResponse.success(new OnboardingStatusResponse(
                                user.getOnboardingCompleted(),
                                user.getName() != null && !user.getName().equals(user.getPhone()),
                                user.getInterests() != null && user.getInterests().length > 0,
                                user.getLatitude() != null && user.getLongitude() != null,
                                user.getHomeHub() != null)));
        }

        // ==================== INTERESTS ====================

        @PutMapping("/me/interests")
        public ResponseEntity<ApiResponse<UserProfileResponse>> updateInterests(
                        @Valid @RequestBody UpdateInterestsRequest request,
                        Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);
                User user = onboardingService.updateInterests(userId, request.interests());
                return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
        }

        // ==================== LOCATION ====================

        @PutMapping("/me/location")
        public ResponseEntity<ApiResponse<UserProfileResponse>> updateLocation(
                        @Valid @RequestBody UpdateLocationRequest request,
                        Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);

                User user = onboardingService.updateLocation(
                                userId,
                                request.latitude(),
                                request.longitude(),
                                request.reassignHub() != null && request.reassignHub());

                return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
        }

        @PutMapping("/me/home-hub")
        public ResponseEntity<ApiResponse<UserProfileResponse>> setHomeHub(
                        @Valid @RequestBody SetHomeHubRequest request,
                        Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);
                User user = onboardingService.setHomeHub(userId, request.hubId());
                return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
        }

        // ==================== TRUST SCORE ====================

        @GetMapping("/me/trust-score")
        public ResponseEntity<ApiResponse<TrustScoreDto>> getMyTrustScore(Authentication authentication) {
                Long userId = authenticatedUserService.requireUserId(authentication);
                TrustScoreDto trustScore = trustScoreService.calculateTrustScore(userId);
                return ResponseEntity.ok(ApiResponse.success(trustScore));
        }

        @GetMapping("/{userId}/trust-score")
        public ResponseEntity<ApiResponse<TrustScoreDto>> getUserTrustScore(@PathVariable Long userId) {
                TrustScoreDto trustScore = trustScoreService.calculateTrustScore(userId);
                return ResponseEntity.ok(ApiResponse.success(trustScore));
        }

        // ==================== HELPERS ====================

        private UserProfileResponse toProfileResponse(User user) {
                return new UserProfileResponse(
                                user.getId(),
                                user.getName(),
                                user.getPhone(),
                                user.getBio(),
                                user.getAvatarUrl(),
                                user.getInterests() != null ? List.of(user.getInterests()) : List.of(),
                                user.getLatitude(),
                                user.getLongitude(),
                                user.getHomeHub() != null ? user.getHomeHub().getId() : null,
                                user.getHomeHub() != null ? user.getHomeHub().getName() : null,
                                user.getOnboardingCompleted(),
                                user.getVerified(),
                                user.getCreatedAt().toString());
        }

        // ==================== DTOs ====================

        public record UserProfileResponse(
                        Long id,
                        String name,
                        String phone,
                        String bio,
                        String avatarUrl,
                        List<String> interests,
                        BigDecimal latitude,
                        BigDecimal longitude,
                        Long homeHubId,
                        String homeHubName,
                        Boolean onboardingCompleted,
                        Boolean verified,
                        String createdAt) {
        }

        public record UpdateProfileRequest(
                        String name,
                        String bio,
                        String avatarUrl) {
        }

        public record OnboardingRequest(
                        @NotBlank(message = "Name is required") String name,
                        String bio,
                        String avatarUrl,
                        @NotEmpty(message = "At least one interest is required") String[] interests,
                        BigDecimal latitude,
                        BigDecimal longitude) {
        }

        public record OnboardingStatusResponse(
                        Boolean completed,
                        Boolean hasName,
                        Boolean hasInterests,
                        Boolean hasLocation,
                        Boolean hasHomeHub) {
        }

        public record UpdateInterestsRequest(
                        @NotEmpty(message = "At least one interest is required") String[] interests) {
        }

        public record UpdateLocationRequest(
                        @NotNull(message = "Latitude is required") BigDecimal latitude,
                        @NotNull(message = "Longitude is required") BigDecimal longitude,
                        Boolean reassignHub) {
        }

        public record SetHomeHubRequest(
                        @NotNull(message = "Hub ID is required") Long hubId) {
        }
}
