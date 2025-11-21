package com.gathr.service;

import com.gathr.entity.UserPhoneHash;
import com.gathr.repository.UserPhoneHashRepository;
import com.gathr.repository.UserRepository;
import com.gathr.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final UserPhoneHashRepository userPhoneHashRepository;
    private final UserRepository userRepository;

    public ContactService(UserPhoneHashRepository userPhoneHashRepository, UserRepository userRepository) {
        this.userPhoneHashRepository = userPhoneHashRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> uploadContacts(Long userId, List<String> hashes) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Delete existing hashes for this user
        userPhoneHashRepository.deleteByUserId(userId);

        // Save new hashes
        List<UserPhoneHash> phoneHashes = hashes.stream()
                .map(hash -> {
                    UserPhoneHash userPhoneHash = new UserPhoneHash();
                    userPhoneHash.setUser(userRepository.getReferenceById(userId));
                    userPhoneHash.setPhoneHash(hash);
                    return userPhoneHash;
                })
                .collect(Collectors.toList());

        userPhoneHashRepository.saveAll(phoneHashes);

        // Calculate mutuals count
        Set<String> userHashes = hashes.stream().collect(Collectors.toSet());
        Long mutualsCount = userPhoneHashRepository.countMutualUsers(userHashes);

        Map<String, Object> response = new HashMap<>();
        response.put("mutualsCount", mutualsCount);
        return response;
    }

    @Transactional(readOnly = true)
    public Long getMutualsCount(Long userId, Long otherUserId) {
        Set<String> userHashes = userPhoneHashRepository.findPhoneHashesByUserId(userId);
        if (userHashes.isEmpty()) {
            return 0L;
        }
        return userPhoneHashRepository.countMutualUsers(userHashes);
    }
}

