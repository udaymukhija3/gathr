package com.gathr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "promo")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "terms_conditions", columnDefinition = "TEXT")
    private String termsConditions;

    @Column(name = "discount_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_spend", precision = 10, scale = 2)
    private BigDecimal minSpend;

    @Column(name = "target_categories", columnDefinition = "text array")
    private String[] targetCategories;

    @Column(name = "target_day_of_week", columnDefinition = "INT[]")
    private Integer[] targetDayOfWeek; // 0=Sunday, 1=Monday, etc.

    @Column(name = "target_time_start")
    private Integer targetTimeStart; // Minutes from midnight

    @Column(name = "target_time_end")
    private Integer targetTimeEnd;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "max_redemptions_per_user")
    private Integer maxRedemptionsPerUser = 1;

    @Column(name = "current_redemptions", nullable = false)
    private Integer currentRedemptions = 0;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DiscountType {
        PERCENTAGE, // X% off
        FIXED_AMOUNT, // $X off
        BOGO, // Buy one get one
        FREE_ITEM // Free item with purchase
    }

    /**
     * Check if promotion is currently valid.
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive &&
                now.isAfter(startsAt) &&
                now.isBefore(expiresAt) &&
                (maxRedemptions == null || currentRedemptions < maxRedemptions);
    }

    /**
     * Check if promotion is valid for given day and time.
     */
    public boolean isValidForDayAndTime(int dayOfWeek, int minutesFromMidnight) {
        if (!isValid())
            return false;

        // Check day of week
        if (targetDayOfWeek != null && targetDayOfWeek.length > 0) {
            boolean dayMatch = false;
            for (Integer day : targetDayOfWeek) {
                if (day == dayOfWeek) {
                    dayMatch = true;
                    break;
                }
            }
            if (!dayMatch)
                return false;
        }

        // Check time range
        if (targetTimeStart != null && targetTimeEnd != null) {
            if (targetTimeStart <= targetTimeEnd) {
                if (minutesFromMidnight < targetTimeStart || minutesFromMidnight > targetTimeEnd) {
                    return false;
                }
            } else {
                // Wraps midnight
                if (minutesFromMidnight < targetTimeStart && minutesFromMidnight > targetTimeEnd) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Increment redemption count.
     */
    public void incrementRedemptions() {
        this.currentRedemptions++;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hub getHub() {
        return hub;
    }

    public void setHub(Hub hub) {
        this.hub = hub;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTermsConditions() {
        return termsConditions;
    }

    public void setTermsConditions(String termsConditions) {
        this.termsConditions = termsConditions;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinSpend() {
        return minSpend;
    }

    public void setMinSpend(BigDecimal minSpend) {
        this.minSpend = minSpend;
    }

    public List<String> getTargetCategories() {
        if (targetCategories == null) {
            return null;
        }
        return List.of(targetCategories);
    }

    public void setTargetCategories(List<String> targetCategories) {
        if (targetCategories == null) {
            this.targetCategories = null;
        } else {
            this.targetCategories = targetCategories.toArray(new String[0]);
        }
    }

    public Integer[] getTargetDayOfWeek() {
        return targetDayOfWeek;
    }

    public void setTargetDayOfWeek(Integer[] targetDayOfWeek) {
        this.targetDayOfWeek = targetDayOfWeek;
    }

    public Integer getTargetTimeStart() {
        return targetTimeStart;
    }

    public void setTargetTimeStart(Integer targetTimeStart) {
        this.targetTimeStart = targetTimeStart;
    }

    public Integer getTargetTimeEnd() {
        return targetTimeEnd;
    }

    public void setTargetTimeEnd(Integer targetTimeEnd) {
        this.targetTimeEnd = targetTimeEnd;
    }

    public Integer getMaxRedemptions() {
        return maxRedemptions;
    }

    public void setMaxRedemptions(Integer maxRedemptions) {
        this.maxRedemptions = maxRedemptions;
    }

    public Integer getMaxRedemptionsPerUser() {
        return maxRedemptionsPerUser;
    }

    public void setMaxRedemptionsPerUser(Integer maxRedemptionsPerUser) {
        this.maxRedemptionsPerUser = maxRedemptionsPerUser;
    }

    public Integer getCurrentRedemptions() {
        return currentRedemptions;
    }

    public void setCurrentRedemptions(Integer currentRedemptions) {
        this.currentRedemptions = currentRedemptions;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
