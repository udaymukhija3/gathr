package com.gathr.service;

import com.gathr.repository.ContactHashRepository;
import com.gathr.entity.ContactHash;
import com.gathr.entity.User;
import com.gathr.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactHashRepository contactHashRepository;
    private final UserRepository userRepository;
    private final EventLogService eventLogService;

    public ContactService(ContactHashRepository contactHashRepository, UserRepository userRepository,
            EventLogService eventLogService) {
        this.contactHashRepository = contactHashRepository;
        this.userRepository = userRepository;
        this.eventLogService = eventLogService;
    }

    @Transactional
    public Map<String, Object> uploadContacts(Long userId, List<String> hashes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Mark user as opted-in
        if (!Boolean.TRUE.equals(user.getContactsOptIn())) {
            user.setContactsOptIn(true);
            userRepository.save(user);
        }

        // 2. Filter existing hashes to avoid duplicates (or rely on ON CONFLICT IGNORE
        // if using native query,
        // but JPA batch save is easier for MVP if volume isn't huge)
        // For MVP, we'll just delete existing and re-insert or ignore duplicates.
        // Let's try a simple approach: fetch existing, filter, save new.

        Set<String> existingHashes = contactHashRepository.findByUserId(userId).stream()
                .map(ContactHash::getPhoneHash)
                .collect(Collectors.toSet());

        List<ContactHash> newHashes = hashes.stream()
                .filter(hash -> !existingHashes.contains(hash))
                .map(hash -> {
                    ContactHash ch = new ContactHash();
                    ch.setUserId(userId);
                    ch.setPhoneHash(hash);
                    ch.setHashAlgo("sha256");
                    return ch;
                })
                .collect(Collectors.toList());

        if (!newHashes.isEmpty()) {
            contactHashRepository.saveAll(newHashes);
        }

        // 3. Log event
        eventLogService.log(userId, "upload_contacts", Map.of("count", hashes.size(), "new", newHashes.size()));

        return Map.of(
                "processed", hashes.size(),
                "new", newHashes.size());
    }
}
