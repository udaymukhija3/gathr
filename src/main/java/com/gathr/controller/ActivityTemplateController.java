package com.gathr.controller;

import com.gathr.dto.ActivityTemplateDto;
import com.gathr.dto.CreateTemplateRequest;
import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.ActivityTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/templates")
public class ActivityTemplateController {

    private final ActivityTemplateService templateService;
    private final AuthenticatedUserService authenticatedUserService;

    public ActivityTemplateController(ActivityTemplateService templateService, AuthenticatedUserService authenticatedUserService) {
        this.templateService = templateService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<List<ActivityTemplateDto>> getTemplates(
            @RequestParam(required = false, defaultValue = "all") String type,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        List<ActivityTemplateDto> templates;
        switch (type) {
            case "system":
                templates = templateService.getSystemTemplates();
                break;
            case "user":
                templates = templateService.getUserTemplates(userId);
                break;
            case "all":
            default:
                templates = templateService.getAvailableTemplates(userId);
                break;
        }

        return ResponseEntity.ok(templates);
    }

    @PostMapping
    public ResponseEntity<ActivityTemplateDto> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        ActivityTemplateDto template = templateService.createTemplate(request, userId);
        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTemplate(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        templateService.deleteTemplate(id, userId);
        return ResponseEntity.ok(new MessageResponse("Template deleted successfully"));
    }

    private record MessageResponse(String message) {}
}
