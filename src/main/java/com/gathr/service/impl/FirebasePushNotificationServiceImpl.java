package com.gathr.service.impl;

import com.gathr.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging implementation of push notification service.
 *
 * To use this implementation:
 * 1. Set push.provider=firebase in application.properties
 * 2. Configure firebase.server-key with your FCM server key
 *
 * Note: For production, consider using Firebase Admin SDK instead of HTTP v1 API.
 */
@Service
@ConditionalOnProperty(name = "push.provider", havingValue = "firebase")
public class FirebasePushNotificationServiceImpl implements PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FirebasePushNotificationServiceImpl.class);
    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";

    @Value("${firebase.server-key:}")
    private String serverKey;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        if (serverKey.isEmpty()) {
            logger.error("Firebase server key not configured! Set firebase.server-key in application.properties");
            return;
        }

        this.webClient = WebClient.builder()
                .baseUrl(FCM_API_URL)
                .defaultHeader("Authorization", "key=" + serverKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        logger.info("Firebase Push Notification Service initialized");
    }

    @Override
    public boolean sendToDevice(String deviceToken, String title, String body, Map<String, String> data) {
        if (webClient == null) {
            logger.error("Firebase not initialized - cannot send notification");
            return false;
        }

        try {
            Map<String, Object> payload = buildPayload(deviceToken, null, title, body, data);

            webClient.post()
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnSuccess(response -> logger.info("FCM response for device {}: {}", maskToken(deviceToken), response))
                    .doOnError(error -> logger.error("FCM error for device {}: {}", maskToken(deviceToken), error.getMessage()))
                    .subscribe();

            return true;
        } catch (Exception e) {
            logger.error("Failed to send FCM notification to device {}: {}", maskToken(deviceToken), e.getMessage());
            return false;
        }
    }

    @Override
    public int sendToDevices(List<String> deviceTokens, String title, String body, Map<String, String> data) {
        if (webClient == null || deviceTokens.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        // FCM supports up to 1000 tokens per request
        int batchSize = 500;

        for (int i = 0; i < deviceTokens.size(); i += batchSize) {
            List<String> batch = deviceTokens.subList(i, Math.min(i + batchSize, deviceTokens.size()));

            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("registration_ids", batch);
                payload.put("notification", Map.of(
                        "title", title,
                        "body", body,
                        "sound", "default"
                ));
                if (data != null && !data.isEmpty()) {
                    payload.put("data", data);
                }

                webClient.post()
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .doOnSuccess(response -> {
                            logger.info("FCM batch response: {}", response);
                        })
                        .doOnError(error -> logger.error("FCM batch error: {}", error.getMessage()))
                        .subscribe();

                successCount += batch.size();
            } catch (Exception e) {
                logger.error("Failed to send FCM batch notification: {}", e.getMessage());
            }
        }

        return successCount;
    }

    @Override
    public boolean sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (webClient == null) {
            return false;
        }

        try {
            Map<String, Object> payload = buildPayload(null, "/topics/" + topic, title, body, data);

            webClient.post()
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .doOnSuccess(response -> logger.info("FCM topic response for {}: {}", topic, response))
                    .doOnError(error -> logger.error("FCM topic error for {}: {}", topic, error.getMessage()))
                    .subscribe();

            return true;
        } catch (Exception e) {
            logger.error("Failed to send FCM notification to topic {}: {}", topic, e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildPayload(String token, String topic, String title, String body, Map<String, String> data) {
        Map<String, Object> payload = new HashMap<>();

        if (token != null) {
            payload.put("to", token);
        } else if (topic != null) {
            payload.put("to", topic);
        }

        payload.put("notification", Map.of(
                "title", title,
                "body", body,
                "sound", "default"
        ));

        if (data != null && !data.isEmpty()) {
            payload.put("data", data);
        }

        // iOS specific
        payload.put("content_available", true);
        payload.put("priority", "high");

        return payload;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
