package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @Column(name = "place_id", length = 191)
    private String placeId;

    @Column(name = "place_name", length = 255)
    private String placeName;

    @Column(name = "place_address", columnDefinition = "TEXT")
    private String placeAddress;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_user_location", nullable = false)
    private Boolean isUserLocation = false;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityCategory category;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "is_invite_only", nullable = false)
    private Boolean isInviteOnly = false;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 4;

    @Column(name = "reveal_identities", nullable = false)
    private Boolean revealIdentities = false;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityStatus status = ActivityStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ActivityCategory {
        SPORTS,
        FOOD,
        ART,
        MUSIC,
        OUTDOOR,
        GAMES,
        LEARNING,
        WELLNESS
    }

    public enum ActivityStatus {
        SCHEDULED,  // Activity is scheduled but hasn't started
        ACTIVE,     // Activity is currently happening
        COMPLETED,  // Activity has ended normally
        CANCELLED   // Activity was cancelled
    }

    public double distanceTo(double targetLat, double targetLng) {
        if (latitude == null || longitude == null) {
            return Double.MAX_VALUE;
        }
        final int EARTH_RADIUS_KM = 6371;
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(targetLat);
        double deltaLat = Math.toRadians(targetLat - latitude);
        double deltaLng = Math.toRadians(targetLng - longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}

