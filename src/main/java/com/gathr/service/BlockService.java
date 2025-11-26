package com.gathr.service;

import com.gathr.entity.Block;
import com.gathr.entity.User;
import com.gathr.repository.BlockRepository;
import com.gathr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final EventLogService eventLogService;

    public BlockService(BlockRepository blockRepository, UserRepository userRepository,
            EventLogService eventLogService) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.eventLogService = eventLogService;
    }

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return; // Already blocked
        }

        User blocker = userRepository.findById(blockerId).orElseThrow();
        User blocked = userRepository.findById(blockedId).orElseThrow();

        Block block = new Block();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        blockRepository.save(block);

        eventLogService.log(blockerId, "block_user", java.util.Map.of("blockedId", blockedId));
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getBlockedUserIds(Long userId) {
        return blockRepository.findByBlockerId(userId).stream()
                .map(block -> block.getBlocked().getId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<Long> getBlockingUserIds(Long userId) {
        return blockRepository.findByBlockedId(userId).stream()
                .map(block -> block.getBlocker().getId())
                .collect(Collectors.toSet());
    }
}
