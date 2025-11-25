package com.gathr.controller;

import com.gathr.dto.CreateMessageRequest;
import com.gathr.dto.MessageDto;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/activities/{activityId}/messages")
public class MessageController {

    private final MessageService messageService;
    private final AuthenticatedUserService authenticatedUserService;

    public MessageController(MessageService messageService, AuthenticatedUserService authenticatedUserService) {
        this.messageService = messageService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long activityId) {
        List<MessageDto> messages = messageService.getMessagesByActivity(activityId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping
    public ResponseEntity<MessageDto> createMessage(
            @PathVariable Long activityId,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        MessageDto message = messageService.createMessage(activityId, userId, request);
        return ResponseEntity.ok(message);
    }
}

