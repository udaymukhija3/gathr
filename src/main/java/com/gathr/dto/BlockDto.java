package com.gathr.dto;

import com.gathr.entity.Block;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Block entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockDto {

    private Long id;
    private Long blockerId;
    private String blockerName;
    private Long blockedId;
    private String blockedName;
    private LocalDateTime createdAt;
    private String reason;

    /**
     * Convert Block entity to DTO
     */
    public static BlockDto fromEntity(Block block) {
        return BlockDto.builder()
            .id(block.getId())
            .blockerId(block.getBlocker().getId())
            .blockerName(block.getBlocker().getName())
            .blockedId(block.getBlocked().getId())
            .blockedName(block.getBlocked().getName())
            .createdAt(block.getCreatedAt())
            .reason(block.getReason())
            .build();
    }
}
