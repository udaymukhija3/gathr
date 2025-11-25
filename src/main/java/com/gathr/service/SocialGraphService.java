package com.gathr.service;

import com.gathr.entity.Participation;
import com.gathr.entity.SocialConnection;
import com.gathr.entity.SocialConnection.ConnectionType;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ParticipationRepository;
import com.gathr.repository.SocialConnectionRepository;
import com.gathr.repository.UserPhoneHashRepository;
import com.gathr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Social graph helper service.
 *
 * Phase 2 implementation focuses on:
 * - Approximating mutual contacts per activity based on uploaded phone hashes.
 *
 * Definition (approximate):
 * A participant is counted as a "mutual" for viewer if the viewer has that participant's
 * phone number in their uploaded contacts (after normalization + hashing).
 */
@Service
public class SocialGraphService {

    private final UserRepository userRepository;
    private final UserPhoneHashRepository userPhoneHashRepository;
    private final ParticipationRepository participationRepository;
    private final SocialConnectionRepository socialConnectionRepository;

    public SocialGraphService(
            UserRepository userRepository,
            UserPhoneHashRepository userPhoneHashRepository,
            ParticipationRepository participationRepository,
            SocialConnectionRepository socialConnectionRepository
    ) {
        this.userRepository = userRepository;
        this.userPhoneHashRepository = userPhoneHashRepository;
        this.participationRepository = participationRepository;
        this.socialConnectionRepository = socialConnectionRepository;
    }

    /**
     * Approximate how many participants in the activity are in the viewer's contacts.
     */
    @Transactional(readOnly = true)
    public int getMutualCountForActivity(Long viewerUserId, Long activityId) {
        userRepository.findById(viewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", viewerUserId));

        // Hashes of viewer's contacts (uploaded from device)
        Set<String> viewerContactHashes = userPhoneHashRepository.findPhoneHashesByUserId(viewerUserId);
        if (viewerContactHashes.isEmpty()) {
            return 0;
        }

        // Participants (INTERESTED or CONFIRMED) with user eagerly loaded
        List<Participation> interested = participationRepository.findByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.INTERESTED);
        List<Participation> confirmed = participationRepository.findByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.CONFIRMED);

        Set<Long> countedUserIds = new HashSet<>();
        int mutuals = 0;

        for (Participation p : concat(interested, confirmed)) {
            User participant = p.getUser();
            if (participant == null) {
                continue;
            }

            Long participantId = participant.getId();
            if (participantId == null || participantId.equals(viewerUserId)) {
                continue;
            }

            if (countedUserIds.contains(participantId)) {
                continue;
            }

            String phone = participant.getPhone();
            if (phone == null || phone.isBlank()) {
                continue;
            }

            String phoneHash = hashPhoneNumber(phone);
            if (viewerContactHashes.contains(phoneHash)) {
                mutuals++;
                countedUserIds.add(participantId);
            }
        }

        return mutuals;
    }

    private List<Participation> concat(List<Participation> a, List<Participation> b) {
        return new java.util.ArrayList<Participation>() {{
            addAll(a);
            addAll(b);
        }};
    }

    /**
     * Normalize and hash a phone number using the same logic as the mobile client.
     * Mirrors frontend's normalizePhoneNumber + hashPhoneNumber in contactHashing.ts.
     */
    String hashPhoneNumber(String phone) {
        String normalized = normalizePhoneNumber(phone);
        return sha256Hex(normalized.toLowerCase().trim());
    }

    String normalizePhoneNumber(String phone) {
        // Remove all non-digit characters
        String cleaned = phone.replaceAll("\\D", "");

        // If starts with 0, replace with country code (India: +91)
        if (cleaned.startsWith("0")) {
            cleaned = "91" + cleaned.substring(1);
        }

        // If doesn't start with country code and is 10 digits, assume India (+91)
        if (cleaned.length() == 10) {
            cleaned = "91" + cleaned;
        }

        // Ensure it starts with +
        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true)
    public long countApproximateMutualConnections(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Set<String> hashes = userPhoneHashRepository.findPhoneHashesByUserId(userId);
        if (hashes.isEmpty()) {
            return 0L;
        }
        Long matches = userPhoneHashRepository.countMutualUsers(hashes);
        if (matches == null) {
            return 0L;
        }
        // Subtract one to exclude the current user's own uploads from the count
        return Math.max(0L, matches - 1);
    }

    @Transactional
    public void refreshConnectionsForActivity(Long activityId) {
        List<Participation> confirmed = participationRepository.findByActivityIdAndStatus(
                activityId, Participation.ParticipationStatus.CONFIRMED);
        List<User> participants = confirmed.stream()
                .map(Participation::getUser)
                .filter(user -> user != null && user.getId() != null)
                .distinct()
                .toList();

        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                User first = participants.get(i);
                User second = participants.get(j);
                upsertConnection(first, second, ConnectionType.ATTENDED_TOGETHER);
                upsertConnection(second, first, ConnectionType.ATTENDED_TOGETHER);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<User> findFriendsOfFriends(Long userId, int maxDepth) {
        User root = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (maxDepth < 1) {
            return List.of();
        }

        Set<Long> visited = new HashSet<>();
        List<User> suggestions = new ArrayList<>();
        ArrayDeque<NodeDepth> queue = new ArrayDeque<>();
        queue.add(new NodeDepth(root.getId(), 0));
        visited.add(root.getId());

        while (!queue.isEmpty()) {
            NodeDepth current = queue.poll();
            if (current.depth() >= maxDepth) {
                continue;
            }

            List<SocialConnection> neighbors = socialConnectionRepository.findBySourceUserId(current.userId());
            for (SocialConnection connection : neighbors) {
                Long targetId = connection.getTargetUser().getId();
                if (targetId == null || visited.contains(targetId)) {
                    continue;
                }
                visited.add(targetId);
                if (current.depth() + 1 > 0) {
                    suggestions.add(connection.getTargetUser());
                }
                queue.offer(new NodeDepth(targetId, current.depth() + 1));
            }
        }

        return suggestions;
    }

    @Transactional(readOnly = true)
    public double calculateConnectionStrength(Long sourceUserId, Long targetUserId) {
        if (sourceUserId.equals(targetUserId)) {
            return 1.0;
        }

        userRepository.findById(sourceUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", sourceUserId));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        double strength = 0.0;
        strength += getConnectionStrength(sourceUserId, targetUserId, ConnectionType.ATTENDED_TOGETHER);
        if (isInContactList(sourceUserId, target)) {
            strength += 0.3;
        }
        return Math.min(1.0, strength);
    }

    private double getConnectionStrength(Long sourceUserId, Long targetUserId, ConnectionType type) {
        return socialConnectionRepository.findBySourceUserIdAndTargetUserIdAndType(sourceUserId, targetUserId, type)
                .map(SocialConnection::getStrength)
                .orElse(0.0);
    }

    private boolean isInContactList(Long sourceUserId, User potentialContact) {
        if (potentialContact.getPhone() == null || potentialContact.getPhone().isBlank()) {
            return false;
        }
        Set<String> hashes = userPhoneHashRepository.findPhoneHashesByUserId(sourceUserId);
        if (hashes.isEmpty()) {
            return false;
        }
        String contactHash = hashPhoneNumber(potentialContact.getPhone());
        return hashes.contains(contactHash);
    }

    private void upsertConnection(User source, User target, ConnectionType type) {
        if (source.getId().equals(target.getId())) {
            return;
        }

        SocialConnection connection = socialConnectionRepository
                .findBySourceUserIdAndTargetUserIdAndType(source.getId(), target.getId(), type)
                .orElseGet(() -> {
                    SocialConnection sc = new SocialConnection();
                    sc.setSourceUser(source);
                    sc.setTargetUser(target);
                    sc.setType(type);
                    sc.setStrength(0.0);
                    sc.setInteractionCount(0);
                    return sc;
                });

        connection.setInteractionCount(connection.getInteractionCount() + 1);
        connection.setStrength(Math.min(1.0, connection.getStrength() + 0.1));
        connection.setLastInteractedAt(LocalDateTime.now());
        socialConnectionRepository.save(connection);
    }

    private record NodeDepth(Long userId, int depth) {
    }
}


