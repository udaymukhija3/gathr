package com.gathr.controller;

import com.gathr.dto.CreateReportRequest;
import com.gathr.entity.Report;
import com.gathr.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<?> createReport(
            @Valid @RequestBody CreateReportRequest request,
            Authentication authentication) {
        Long reporterId = (Long) authentication.getPrincipal();
        
        Report report = reportService.createReport(
                reporterId,
                request.getTargetUserId(),
                request.getActivityId(),
                request.getReason()
        );

        return ResponseEntity.ok().body(new MessageResponse("Report created successfully", report.getId()));
    }

    private record MessageResponse(String message, Long reportId) {}
}

