package com.gathr.controller;

import com.gathr.security.AuthenticatedUserService;
import com.gathr.service.BlockService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blocks")
public class BlockController {

    private final BlockService blockService;
    private final AuthenticatedUserService authenticatedUserService;

    public BlockController(BlockService blockService, AuthenticatedUserService authenticatedUserService) {
        this.blockService = blockService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Void> blockUser(@PathVariable Long userId, Authentication authentication) {
        Long blockerId = authenticatedUserService.requireUserId(authentication);
        blockService.blockUser(blockerId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId, Authentication authentication) {
        Long blockerId = authenticatedUserService.requireUserId(authentication);
        blockService.unblockUser(blockerId, userId);
        return ResponseEntity.ok().build();
    }
}
