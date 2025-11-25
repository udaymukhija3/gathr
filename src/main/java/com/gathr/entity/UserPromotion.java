package com.gathr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks user interactions with promotions.
 * Supports the full funnel: view -> click -> save -> redeem.
 */
@Entity
@Table(name = "user_promotions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "promotion_id"}))
public class UserPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity; // Activity where promotion was redeemed

    @Column(name = "redemption_amount", precision = 10, scale = 2)
    private BigDecimal redemptionAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void markViewed() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }

    public void markClicked() {
        if (clickedAt == null) {
            clickedAt = LocalDateTime.now();
        }
    }

    public void markSaved() {
        savedAt = LocalDateTime.now();
    }

    public void unsave() {
        savedAt = null;
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    public boolean isSaved() {
        return savedAt != null;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public PromoCode getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(PromoCode promoCode) {
        this.promoCode = promoCode;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public LocalDateTime getClickedAt() {
        return clickedAt;
    }

    public void setClickedAt(LocalDateTime clickedAt) {
        this.clickedAt = clickedAt;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public BigDecimal getRedemptionAmount() {
        return redemptionAmount;
    }

    public void setRedemptionAmount(BigDecimal redemptionAmount) {
        this.redemptionAmount = redemptionAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
