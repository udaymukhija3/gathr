package com.gathr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_connections", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"source_user_id", "target_user_id", "connection_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_user_id", nullable = false)
    private User sourceUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", length = 40, nullable = false)
    private ConnectionType type;

    @Column(name = "strength", nullable = false)
    private Double strength = 0.0;

    @Column(name = "interaction_count", nullable = false)
    private Integer interactionCount = 0;

    @Column(name = "last_interacted_at")
    private LocalDateTime lastInteractedAt;

    public enum ConnectionType {
        MUTUAL_CONTACT,
        ATTENDED_TOGETHER
    }
}

