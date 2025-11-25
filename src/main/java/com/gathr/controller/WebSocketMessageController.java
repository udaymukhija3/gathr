package com.gathr.controller;

import com.gathr.dto.CreateMessageRequest;
import com.gathr.dto.MessageDto;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketMessageController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthenticatedUserService authenticatedUserService;

    public WebSocketMessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate, AuthenticatedUserService authenticatedUserService) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.authenticatedUserService = authenticatedUserService;
    }

    @MessageMapping("/activities/{activityId}/messages")
    public void sendMessage(
            @DestinationVariable Long activityId,
            @Payload CreateMessageRequest request,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Unauthenticated WebSocket message attempt for activity: {}", activityId);
            return;
        }

        try {
            Long userId = authenticatedUserService.requireUserId(authentication);
            logger.info("Received WebSocket message from user {} for activity {}", userId, activityId);

            // Save message to database
            MessageDto savedMessage = messageService.createMessage(activityId, userId, request);

            // Broadcast message to all subscribers of this activity
            messagingTemplate.convertAndSend(
                    "/topic/activities/" + activityId + "/messages",
                    savedMessage
            );

            logger.info("Message broadcasted to activity {}", activityId);
        } catch (Exception e) {
            logger.error("Error processing WebSocket message: {}", e.getMessage(), e);

            // Send error back to user
            messagingTemplate.convertAndSendToUser(
                    authentication.getName(),
                    "/queue/errors",
                    "Failed to send message: " + e.getMessage()
            );
        }
    }

    @MessageMapping("/activities/{activityId}/typing")
    public void userTyping(
            @DestinationVariable Long activityId,
            Authentication authentication) {

        if (authentication != null && authentication.getPrincipal() != null) {
            Long userId = authenticatedUserService.requireUserId(authentication);

            // Broadcast typing indicator to other users
            messagingTemplate.convertAndSend(
                    "/topic/activities/" + activityId + "/typing",
                    userId
            );
        }
    }
}
