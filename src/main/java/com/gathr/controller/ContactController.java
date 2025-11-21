package com.gathr.controller;

import com.gathr.service.ContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadContacts(
            @Valid @RequestBody ContactUploadRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> response = contactService.uploadContacts(userId, request.getHashes());
        return ResponseEntity.ok(response);
    }

    public static class ContactUploadRequest {
        @NotEmpty(message = "Hashes list cannot be empty")
        private List<String> hashes;

        public List<String> getHashes() {
            return hashes;
        }

        public void setHashes(List<String> hashes) {
            this.hashes = hashes;
        }
    }
}

