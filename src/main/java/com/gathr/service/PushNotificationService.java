package com.gathr.service;

import java.util.List;
import java.util.Map;

/**
 * Interface for push notification delivery.
 * Implementations: MockPushNotificationService (dev), FirebasePushNotificationService (prod)
 */
public interface PushNotificationService {

    /**
     * Send notification to a single device.
     *
     * @param deviceToken FCM/APNs device token
     * @param title Notification title
     * @param body Notification body
     * @param data Additional payload data
     * @return true if sent successfully
     */
    boolean sendToDevice(String deviceToken, String title, String body, Map<String, String> data);

    /**
     * Send notification to multiple devices.
     *
     * @param deviceTokens List of FCM/APNs device tokens
     * @param title Notification title
     * @param body Notification body
     * @param data Additional payload data
     * @return Number of successful sends
     */
    int sendToDevices(List<String> deviceTokens, String title, String body, Map<String, String> data);

    /**
     * Send notification to a topic (e.g., all users interested in FOOD category).
     *
     * @param topic Topic name
     * @param title Notification title
     * @param body Notification body
     * @param data Additional payload data
     * @return true if sent successfully
     */
    boolean sendToTopic(String topic, String title, String body, Map<String, String> data);
}
