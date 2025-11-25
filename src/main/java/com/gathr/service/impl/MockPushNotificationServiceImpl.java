package com.gathr.service.impl;

import com.gathr.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Mock implementation of push notification service for development/testing.
 * Logs notifications instead of sending them.
 */
@Service
@ConditionalOnProperty(name = "push.provider", havingValue = "mock", matchIfMissing = true)
public class MockPushNotificationServiceImpl implements PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(MockPushNotificationServiceImpl.class);

    @Override
    public boolean sendToDevice(String deviceToken, String title, String body, Map<String, String> data) {
        logger.info("[MOCK PUSH] To device: {} | Title: {} | Body: {} | Data: {}",
                maskToken(deviceToken), title, body, data);
        return true;
    }

    @Override
    public int sendToDevices(List<String> deviceTokens, String title, String body, Map<String, String> data) {
        logger.info("[MOCK PUSH] To {} devices | Title: {} | Body: {} | Data: {}",
                deviceTokens.size(), title, body, data);
        deviceTokens.forEach(token ->
            logger.debug("[MOCK PUSH] Device: {}", maskToken(token))
        );
        return deviceTokens.size();
    }

    @Override
    public boolean sendToTopic(String topic, String title, String body, Map<String, String> data) {
        logger.info("[MOCK PUSH] To topic: {} | Title: {} | Body: {} | Data: {}",
                topic, title, body, data);
        return true;
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) return "***";
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
}
