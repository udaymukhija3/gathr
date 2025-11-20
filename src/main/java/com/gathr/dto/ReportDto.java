package com.gathr.dto;

import com.gathr.entity.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Report entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDto {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private Long targetUserId;
    private String targetUserName;
    private Long messageId;
    private Long activityId;
    private String reason;
    private String reasonDisplay;
    private String details;
    private String status;
    private String statusDisplay;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private Long reviewedById;
    private String reviewedByName;
    private String adminNotes;

    /**
     * Convert Report entity to DTO
     */
    public static ReportDto fromEntity(Report report) {
        return ReportDto.builder()
            .id(report.getId())
            .reporterId(report.getReporter().getId())
            .reporterName(report.getReporter().getName())
            .targetUserId(report.getTargetUser().getId())
            .targetUserName(report.getTargetUser().getName())
            .messageId(report.getMessage() != null ? report.getMessage().getId() : null)
            .activityId(report.getActivity() != null ? report.getActivity().getId() : null)
            .reason(report.getReason().name())
            .reasonDisplay(report.getReason().getDisplayName())
            .details(report.getDetails())
            .status(report.getStatus().name())
            .statusDisplay(report.getStatus().getDisplayName())
            .createdAt(report.getCreatedAt())
            .reviewedAt(report.getReviewedAt())
            .reviewedById(report.getReviewedBy() != null ? report.getReviewedBy().getId() : null)
            .reviewedByName(report.getReviewedBy() != null ? report.getReviewedBy().getName() : null)
            .adminNotes(report.getAdminNotes())
            .build();
    }

    /**
     * Convert Report entity to DTO (simplified, without sensitive info)
     */
    public static ReportDto fromEntitySimplified(Report report) {
        return ReportDto.builder()
            .id(report.getId())
            .reason(report.getReason().name())
            .reasonDisplay(report.getReason().getDisplayName())
            .status(report.getStatus().name())
            .statusDisplay(report.getStatus().getDisplayName())
            .createdAt(report.getCreatedAt())
            .build();
    }
}
