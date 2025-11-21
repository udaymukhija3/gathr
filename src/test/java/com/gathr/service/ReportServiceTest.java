package com.gathr.service;

import com.gathr.entity.Report;
import com.gathr.entity.User;
import com.gathr.entity.Activity;
import com.gathr.repository.ReportRepository;
import com.gathr.repository.UserRepository;
import com.gathr.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private EventLogService eventLogService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private ReportService reportService;

    private User reporter;
    private User targetUser;
    private Activity activity;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(1L);
        reporter.setName("Reporter");

        targetUser = new User();
        targetUser.setId(2L);
        targetUser.setName("Target");

        activity = new Activity();
        activity.setId(1L);
        activity.setTitle("Test Activity");

        when(webClientBuilder.build()).thenReturn(mock(WebClient.class));
    }

    @Test
    void testCreateReport() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        
        Report savedReport = new Report();
        savedReport.setId(1L);
        savedReport.setReporter(reporter);
        savedReport.setTargetUser(targetUser);
        savedReport.setActivity(activity);
        savedReport.setReason("Test reason");
        savedReport.setStatus("OPEN");
        
        when(reportRepository.save(any(Report.class))).thenReturn(savedReport);

        // When
        Report result = reportService.createReport(1L, 2L, 1L, "Test reason");

        // Then
        assertNotNull(result);
        assertEquals("OPEN", result.getStatus());
        assertEquals("Test reason", result.getReason());
        verify(reportRepository, times(1)).save(any(Report.class));
        verify(eventLogService, times(1)).log(anyLong(), anyLong(), eq("report_created"), any());
    }
}

