package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    // Onboarding fields
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    @Column(name = "interests", columnDefinition = "text array")
    private String[] interests; // SPORTS, FOOD, ART, MUSIC

    @Column(length = 500)
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    // Location fields
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_hub_id")
    private Hub homeHub;

    @Column(name = "home_latitude", precision = 10, scale = 8)
    private BigDecimal homeLatitude;

    @Column(name = "home_longitude", precision = 11, scale = 8)
    private BigDecimal homeLongitude;

    @Column(name = "preferred_radius_km")
    private Integer preferredRadiusKm = 10;

    @Column(name = "preferred_start_hour")
    private Integer preferredStartHour;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Safety & Trust fields
    @Column(name = "contacts_opt_in")
    private Boolean contactsOptIn = false;

    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, DELETED

    @Column(name = "trust_score")
    private Integer trustScore = 100;

    @Column(name = "reveal_until_participants")
    private Integer revealUntilParticipants = 3;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if user has completed onboarding with required fields.
     */
    public boolean hasCompletedOnboarding() {
        return onboardingCompleted &&
                interests != null && interests.length > 0 &&
                name != null && !name.equals(phone); // Name should be set, not defaulting to phone
    }

    /**
     * Update user's location.
     */
    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationUpdatedAt = LocalDateTime.now();
    }
}
