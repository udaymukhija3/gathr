package com.gathr.controller;

import com.gathr.dto.HubDto;
import com.gathr.service.HubService;
import com.gathr.service.OnboardingService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/hubs")
public class HubController {

    private final HubService hubService;
    private final OnboardingService onboardingService;

    public HubController(HubService hubService, OnboardingService onboardingService) {
        this.hubService = hubService;
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public ResponseEntity<List<HubDto>> getAllHubs() {
        List<HubDto> hubs = hubService.getAllHubs();
        return ResponseEntity.ok(hubs);
    }

    /**
     * Find nearest hubs based on user's coordinates.
     * Used during onboarding and when user wants to discover nearby hubs.
     */
    @GetMapping("/nearest")
    public ResponseEntity<List<HubWithDistanceDto>> getNearestHubs(
            @RequestParam @NotNull BigDecimal latitude,
            @RequestParam @NotNull BigDecimal longitude,
            @RequestParam(defaultValue = "5") int limit) {

        List<HubWithDistanceDto> hubs = onboardingService.findNearestHubs(latitude, longitude, limit)
                .stream()
                .map(hwd -> new HubWithDistanceDto(
                        hwd.hub().getId(),
                        hwd.hub().getName(),
                        hwd.hub().getArea(),
                        hwd.hub().getDescription(),
                        hwd.hub().getLatitude(),
                        hwd.hub().getLongitude(),
                        hwd.hub().getIsPartner(),
                        hwd.hub().getPartnerTier() != null ? hwd.hub().getPartnerTier().name() : null,
                        Math.round(hwd.distanceKm() * 100.0) / 100.0 // Round to 2 decimal places
                ))
                .toList();

        return ResponseEntity.ok(hubs);
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<HubDto> getHub(@PathVariable Long hubId) {
        HubDto hub = hubService.getHubById(hubId);
        return ResponseEntity.ok(hub);
    }

    public record HubWithDistanceDto(
            Long id,
            String name,
            String area,
            String description,
            BigDecimal latitude,
            BigDecimal longitude,
            Boolean isPartner,
            String partnerTier,
            Double distanceKm
    ) {}
}

