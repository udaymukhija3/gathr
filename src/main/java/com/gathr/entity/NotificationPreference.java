package com.gathr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "activity_reminders", nullable = false)
    private Boolean activityReminders = true;

    @Column(name = "chat_messages", nullable = false)
    private Boolean chatMessages = true;

    @Column(name = "promotional_offers", nullable = false)
    private Boolean promotionalOffers = true;

    @Column(name = "max_promotions_per_day", nullable = false)
    private Integer maxPromotionsPerDay = 3;

    @Column(name = "quiet_hours_start")
    private Integer quietHoursStart; // Minutes from midnight

    @Column(name = "quiet_hours_end")
    private Integer quietHoursEnd;

    @Column(name = "interested_categories", columnDefinition = "TEXT[]")
    private String[] interestedCategories;

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

    /**
     * Check if current time is within quiet hours.
     */
    public boolean isQuietHoursActive(int currentMinutesFromMidnight) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }

        if (quietHoursStart <= quietHoursEnd) {
            // Normal range (e.g., 22:00 to 07:00 doesn't wrap)
            return currentMinutesFromMidnight >= quietHoursStart && currentMinutesFromMidnight < quietHoursEnd;
        } else {
            // Wraps midnight (e.g., 22:00 to 07:00)
            return currentMinutesFromMidnight >= quietHoursStart || currentMinutesFromMidnight < quietHoursEnd;
        }
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

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getActivityReminders() {
        return activityReminders;
    }

    public void setActivityReminders(Boolean activityReminders) {
        this.activityReminders = activityReminders;
    }

    public Boolean getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(Boolean chatMessages) {
        this.chatMessages = chatMessages;
    }

    public Boolean getPromotionalOffers() {
        return promotionalOffers;
    }

    public void setPromotionalOffers(Boolean promotionalOffers) {
        this.promotionalOffers = promotionalOffers;
    }

    public Integer getMaxPromotionsPerDay() {
        return maxPromotionsPerDay;
    }

    public void setMaxPromotionsPerDay(Integer maxPromotionsPerDay) {
        this.maxPromotionsPerDay = maxPromotionsPerDay;
    }

    public Integer getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(Integer quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public Integer getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(Integer quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }

    public String[] getInterestedCategories() {
        return interestedCategories;
    }

    public void setInterestedCategories(String[] interestedCategories) {
        this.interestedCategories = interestedCategories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
