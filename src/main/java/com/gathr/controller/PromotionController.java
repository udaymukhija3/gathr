package com.gathr.controller;

import com.gathr.entity.Promotion;
import com.gathr.dto.common.ApiResponse;
import com.gathr.entity.UserPromotion;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.PromotionRepository;
import com.gathr.repository.UserPromotionRepository;
import com.gathr.repository.UserRepository;
import com.gathr.security.AuthenticatedUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for viewing and interacting with promotions.
 */
@RestController
@RequestMapping("/promotions")
public class PromotionController {

    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public PromotionController(
            PromotionRepository promotionRepository,
            UserPromotionRepository userPromotionRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService) {
        this.promotionRepository = promotionRepository;
        this.userPromotionRepository = userPromotionRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getActivePromotions(
            @RequestParam(required = false) String category,
            Authentication authentication) {
        authenticatedUserService.requireUserId(authentication);

        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions;

        if (category != null && !category.isEmpty()) {
            promotions = promotionRepository.findByTargetCategory(category, now);
        } else {
            promotions = promotionRepository.findAvailablePromotions(now);
        }

        List<PromotionDto> dtos = promotions.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{promotionId}")
    @Transactional
    public ResponseEntity<ApiResponse<PromotionDto>> getPromotion(
            @PathVariable Long promotionId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", promotionId));

        // Track view
        trackInteraction(userId, promotion, InteractionType.VIEW);

        return ResponseEntity.ok(ApiResponse.success(toDto(promotion)));
    }

    @PostMapping("/{promotionId}/click")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> trackClick(
            @PathVariable Long promotionId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", promotionId));

        trackInteraction(userId, promotion, InteractionType.CLICK);

        return ResponseEntity.ok(ApiResponse.success(null, "Click tracked"));
    }

    @PostMapping("/{promotionId}/save")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> savePromotion(
            @PathVariable Long promotionId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", promotionId));

        trackInteraction(userId, promotion, InteractionType.SAVE);

        return ResponseEntity.ok(ApiResponse.success(null, "Promotion saved"));
    }

    @DeleteMapping("/{promotionId}/save")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> unsavePromotion(
            @PathVariable Long promotionId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        userPromotionRepository.findByUserIdAndPromotionId(userId, promotionId)
                .ifPresent(up -> {
                    up.unsave();
                    userPromotionRepository.save(up);
                });

        return ResponseEntity.ok(ApiResponse.success(null, "Promotion unsaved"));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getSavedPromotions(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        List<PromotionDto> saved = userPromotionRepository.findSavedByUserId(userId).stream()
                .map(up -> toDto(up.getPromotion()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @GetMapping("/hub/{hubId}")
    public ResponseEntity<ApiResponse<List<PromotionDto>>> getPromotionsByHub(
            @PathVariable Long hubId,
            Authentication authentication) {
        authenticatedUserService.requireUserId(authentication);

        List<PromotionDto> promotions = promotionRepository.findActiveByHubId(hubId).stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(promotions));
    }

    private void trackInteraction(Long userId, Promotion promotion, InteractionType type) {
        UserPromotion userPromotion = userPromotionRepository
                .findByUserIdAndPromotionId(userId, promotion.getId())
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    UserPromotion up = new UserPromotion();
                    up.setUser(user);
                    up.setPromotion(promotion);
                    return up;
                });

        switch (type) {
            case VIEW -> userPromotion.markViewed();
            case CLICK -> userPromotion.markClicked();
            case SAVE -> userPromotion.markSaved();
        }

        userPromotionRepository.save(userPromotion);
    }

    private PromotionDto toDto(Promotion p) {
        return new PromotionDto(
                p.getId(),
                p.getHub().getId(),
                p.getHub().getName(),
                p.getTitle(),
                p.getDescription(),
                p.getDiscountType().name(),
                p.getDiscountValue() != null ? p.getDiscountValue().doubleValue() : null,
                p.getMinSpend() != null ? p.getMinSpend().doubleValue() : null,
                p.getTargetCategories() != null ? p.getTargetCategories() : List.of(),
                p.getStartsAt().toString(),
                p.getExpiresAt().toString(),
                p.getMaxRedemptions(),
                p.getCurrentRedemptions());
    }

    private enum InteractionType {
        VIEW, CLICK, SAVE
    }

    public record PromotionDto(
            Long id,
            Long hubId,
            String hubName,
            String title,
            String description,
            String discountType,
            Double discountValue,
            Double minSpend,
            List<String> targetCategories,
            String startsAt,
            String expiresAt,
            Integer maxRedemptions,
            Integer currentRedemptions) {
    }
}
