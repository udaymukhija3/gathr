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
    private Long activityId;
    private String reason;
    private String reasonDisplay;
    private String status;
    private String statusDisplay;
    private LocalDateTime createdAt;

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
            .activityId(report.getActivity() != null ? report.getActivity().getId() : null)
            .reason(report.getReason())
            .reasonDisplay(report.getReason())
            .status(report.getStatus())
            .statusDisplay(report.getStatus())
            .createdAt(report.getCreatedAt())
            .build();
    }

    /**
     * Convert Report entity to DTO (simplified, without sensitive info)
     */
    public static ReportDto fromEntitySimplified(Report report) {
        return ReportDto.builder()
            .id(report.getId())
            .reason(report.getReason())
            .reasonDisplay(report.getReason())
            .status(report.getStatus())
            .statusDisplay(report.getStatus())
            .createdAt(report.getCreatedAt())
            .build();
    }
}
