package com.gathr.service;

import com.gathr.dto.ActivityTemplateDto;
import com.gathr.dto.CreateTemplateRequest;
import com.gathr.entity.ActivityTemplate;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ActivityTemplateRepository;
import com.gathr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityTemplateService {

    private final ActivityTemplateRepository templateRepository;
    private final UserRepository userRepository;

    public ActivityTemplateService(ActivityTemplateRepository templateRepository,
                                   UserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivityTemplateDto> getAvailableTemplates(Long userId) {
        List<ActivityTemplate> templates = templateRepository.findAvailableTemplatesForUser(userId);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityTemplateDto> getSystemTemplates() {
        List<ActivityTemplate> templates = templateRepository.findSystemTemplates();
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityTemplateDto> getUserTemplates(Long userId) {
        List<ActivityTemplate> templates = templateRepository.findByUserId(userId);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityTemplateDto createTemplate(CreateTemplateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ActivityTemplate template = new ActivityTemplate();
        template.setName(request.getName());
        template.setTitle(request.getTitle());
        template.setCategory(request.getCategory());
        template.setDurationHours(request.getDurationHours());
        template.setDescription(request.getDescription());
        template.setIsSystemTemplate(false); // User templates are never system templates
        template.setCreatedByUser(user);
        template.setIsInviteOnly(request.getIsInviteOnly() != null ? request.getIsInviteOnly() : false);
        template.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 4);

        template = templateRepository.save(template);
        return convertToDto(template);
    }

    @Transactional
    public void deleteTemplate(Long templateId, Long userId) {
        ActivityTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));

        // Only allow deletion of user's own templates (not system templates)
        if (template.getIsSystemTemplate()) {
            throw new IllegalArgumentException("Cannot delete system templates");
        }

        if (!template.getCreatedByUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot delete another user's template");
        }

        templateRepository.delete(template);
    }

    private ActivityTemplateDto convertToDto(ActivityTemplate template) {
        return new ActivityTemplateDto(
            template.getId(),
            template.getName(),
            template.getTitle(),
            template.getCategory(),
            template.getDurationHours(),
            template.getDescription(),
            template.getIsSystemTemplate(),
            template.getIsInviteOnly(),
            template.getMaxMembers()
        );
    }
}
