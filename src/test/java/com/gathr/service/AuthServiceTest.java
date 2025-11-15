package com.gathr.service;

import com.gathr.dto.AuthRequest;
import com.gathr.dto.AuthResponse;
import com.gathr.dto.OtpVerifyRequest;
import com.gathr.entity.User;
import com.gathr.exception.InvalidRequestException;
import com.gathr.repository.UserRepository;
import com.gathr.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private JwtUtil jwtUtil;

    private AuthService authService;

    private User testUser;
    private static final String TEST_PHONE = "1234567890";

    @BeforeEach
    void setUp() {
        // Use real JwtUtil instead of mocking due to Java 23 compatibility issues
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha-algorithms");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);

        authService = new AuthService(userRepository, jwtUtil);

        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone(TEST_PHONE);
        testUser.setName(TEST_PHONE);
        testUser.setVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void startOtp_ShouldLogMockOtp() {
        // Given
        AuthRequest request = new AuthRequest();
        request.setPhone(TEST_PHONE);

        // When
        authService.startOtp(request);

        // Then - no exception should be thrown
        // In real implementation, this would send SMS
    }

    @Test
    void verifyOtp_WithExistingUser_ShouldReturnAuthResponse() {
        // Given
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPhone(TEST_PHONE);
        request.setOtp("123456");

        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(testUser));

        // When
        AuthResponse response = authService.verifyOtp(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isNotEmpty();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getPhone()).isEqualTo(TEST_PHONE);

        verify(userRepository).findByPhone(TEST_PHONE);
    }

    @Test
    void verifyOtp_WithNewUser_ShouldCreateUserAndReturnAuthResponse() {
        // Given
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPhone(TEST_PHONE);
        request.setOtp("123456");

        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        AuthResponse response = authService.verifyOtp(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isNotEmpty();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getPhone()).isEqualTo(TEST_PHONE);

        verify(userRepository).findByPhone(TEST_PHONE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyOtp_WithUnverifiedUser_ShouldMarkUserAsVerified() {
        // Given
        testUser.setVerified(false);

        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPhone(TEST_PHONE);
        request.setOtp("123456");

        when(userRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        AuthResponse response = authService.verifyOtp(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(testUser.getVerified()).isTrue();

        verify(userRepository).save(testUser);
    }

    @Test
    void verifyOtp_WithNullOtp_ShouldThrowException() {
        // Given
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPhone(TEST_PHONE);
        request.setOtp(null);

        // When/Then
        assertThatThrownBy(() -> authService.verifyOtp(request))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Invalid OTP");

        verify(userRepository, never()).findByPhone(anyString());
    }

    @Test
    void verifyOtp_WithEmptyOtp_ShouldThrowException() {
        // Given
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setPhone(TEST_PHONE);
        request.setOtp("   ");

        // When/Then
        assertThatThrownBy(() -> authService.verifyOtp(request))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Invalid OTP");

        verify(userRepository, never()).findByPhone(anyString());
    }
}
