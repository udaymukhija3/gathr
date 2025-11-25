package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hubs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String area;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Partner/Venue fields for monetization
    @Column(name = "is_partner", nullable = false)
    private Boolean isPartner = false;

    @Column(name = "partner_tier", length = 20)
    @Enumerated(EnumType.STRING)
    private PartnerTier partnerTier = PartnerTier.FREE;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // Location coordinates for distance calculation
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    public enum PartnerTier {
        FREE,     // Basic listing, no promotions
        BASIC,    // Can create limited promotions
        PREMIUM   // Full promotion features, priority placement
    }

    public boolean isVerifiedPartner() {
        return isPartner && verifiedAt != null;
    }

    /**
     * Calculate distance to given coordinates using Haversine formula.
     * @return distance in kilometers
     */
    public double distanceTo(BigDecimal lat, BigDecimal lng) {
        if (latitude == null || longitude == null || lat == null || lng == null) {
            return Double.MAX_VALUE;
        }

        final int EARTH_RADIUS_KM = 6371;

        double lat1 = Math.toRadians(latitude.doubleValue());
        double lat2 = Math.toRadians(lat.doubleValue());
        double deltaLat = Math.toRadians(lat.doubleValue() - latitude.doubleValue());
        double deltaLng = Math.toRadians(lng.doubleValue() - longitude.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}

