package com.gathr.service;

import com.gathr.entity.*;
import com.gathr.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing and sending notifications.
 * Handles user preferences, quiet hours, and rate limiting.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final PushNotificationService pushNotificationService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserDeviceRepository userDeviceRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserPromotionRepository userPromotionRepository,
            PushNotificationService pushNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.preferenceRepository = preferenceRepository;
        this.userPromotionRepository = userPromotionRepository;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Send an activity reminder notification.
     */
    @Transactional
    public void sendActivityReminder(User user, Activity activity) {
        if (!canSendNotification(user.getId(), Notification.NotificationType.ACTIVITY_REMINDER)) {
            logger.debug("Cannot send activity reminder to user {} - preferences or quiet hours", user.getId());
            return;
        }

        String title = "Activity Starting Soon!";
        String body = String.format("'%s' starts in 30 minutes", activity.getTitle());

        Map<String, String> data = new HashMap<>();
        data.put("activityId", activity.getId().toString());
        data.put("type", "ACTIVITY_REMINDER");

        sendNotification(user, Notification.NotificationType.ACTIVITY_REMINDER, title, body, data, activity, null);
    }

    /**
     * Send a promotional notification.
     */
    @Transactional
    public void sendPromotionalNotification(User user, Promotion promotion) {
        if (!canSendPromotionalNotification(user.getId())) {
            logger.debug("Cannot send promotional notification to user {} - preferences, quiet hours, or rate limit", user.getId());
            return;
        }

        String title = promotion.getHub().getName() + " - Special Offer!";
        String body = promotion.getTitle();

        Map<String, String> data = new HashMap<>();
        data.put("promotionId", promotion.getId().toString());
        data.put("hubId", promotion.getHub().getId().toString());
        data.put("type", "PROMOTIONAL");

        sendNotification(user, Notification.NotificationType.PROMOTIONAL, title, body, data, null, promotion);
    }

    /**
     * Send a chat message notification.
     */
    @Transactional
    public void sendChatMessageNotification(User user, Activity activity, String senderName, String messagePreview) {
        if (!canSendNotification(user.getId(), Notification.NotificationType.CHAT_MESSAGE)) {
            return;
        }

        String title = activity.getTitle();
        String body = senderName + ": " + truncate(messagePreview, 100);

        Map<String, String> data = new HashMap<>();
        data.put("activityId", activity.getId().toString());
        data.put("type", "CHAT_MESSAGE");

        sendNotification(user, Notification.NotificationType.CHAT_MESSAGE, title, body, data, activity, null);
    }

    /**
     * Core notification sending logic.
     */
    @Transactional
    public void sendNotification(User user, Notification.NotificationType type, String title, String body,
                                  Map<String, String> data, Activity activity, Promotion promotion) {
        // Create notification record
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(data != null ? new HashMap<>(data) : null);
        notification.setActivity(activity);
        notification.setPromotion(promotion);

        notification = notificationRepository.save(notification);

        // Get user's active devices
        List<UserDevice> devices = userDeviceRepository.findActiveByUserId(user.getId());

        if (devices.isEmpty()) {
            logger.debug("No active devices for user {}", user.getId());
            notification.markFailed("No active devices");
            notificationRepository.save(notification);
            return;
        }

        // Send to all devices
        List<String> tokens = devices.stream()
                .map(UserDevice::getDeviceToken)
                .collect(Collectors.toList());

        int sent = pushNotificationService.sendToDevices(tokens, title, body, data);

        if (sent > 0) {
            notification.markSent();
        } else {
            notification.markFailed("Failed to send to any device");
        }

        notificationRepository.save(notification);
        logger.info("Notification sent to user {} on {} devices", user.getId(), sent);
    }

    /**
     * Check if we can send a notification of given type to user.
     */
    private boolean canSendNotification(Long userId, Notification.NotificationType type) {
        NotificationPreference prefs = preferenceRepository.findByUserId(userId).orElse(null);

        // Default to enabled if no preferences set
        if (prefs == null) {
            return true;
        }

        // Check if push is enabled
        if (!prefs.getPushEnabled()) {
            return false;
        }

        // Check type-specific preference
        switch (type) {
            case ACTIVITY_REMINDER:
                if (!prefs.getActivityReminders()) return false;
                break;
            case CHAT_MESSAGE:
                if (!prefs.getChatMessages()) return false;
                break;
            case PROMOTIONAL:
                if (!prefs.getPromotionalOffers()) return false;
                break;
            default:
                break;
        }

        // Check quiet hours
        if (isQuietHours(prefs)) {
            return false;
        }

        return true;
    }

    /**
     * Check if we can send a promotional notification (includes rate limiting).
     */
    private boolean canSendPromotionalNotification(Long userId) {
        if (!canSendNotification(userId, Notification.NotificationType.PROMOTIONAL)) {
            return false;
        }

        NotificationPreference prefs = preferenceRepository.findByUserId(userId).orElse(null);
        int maxPerDay = prefs != null ? prefs.getMaxPromotionsPerDay() : 3;

        // Check daily limit
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        int sentToday = userPromotionRepository.countPromotionalNotificationsSentToday(userId, startOfDay, endOfDay);

        return sentToday < maxPerDay;
    }

    private boolean isQuietHours(NotificationPreference prefs) {
        if (prefs.getQuietHoursStart() == null || prefs.getQuietHoursEnd() == null) {
            return false;
        }

        LocalTime now = LocalTime.now();
        int currentMinutes = now.getHour() * 60 + now.getMinute();

        return prefs.isQuietHoursActive(currentMinutes);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Get unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    /**
     * Mark notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markRead();
            notificationRepository.save(notification);
        });
    }

    /**
     * Get unread count for a user.
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
}
