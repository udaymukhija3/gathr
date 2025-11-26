package com.gathr.service;

import com.gathr.dto.AuthRequest;
import com.gathr.dto.AuthResponse;
import com.gathr.dto.OtpVerifyRequest;
import com.gathr.dto.UserDto;
import com.gathr.entity.User;
import com.gathr.exception.InvalidRequestException;
import com.gathr.repository.UserRepository;
import com.gathr.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for authentication operations.
 * Rate limiting is handled by RateLimitInterceptor at the infrastructure layer.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, OtpService otpService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
    }

    public void startOtp(AuthRequest request) {
        String phone = request.getPhone();

        // Send OTP via configured OtpService (mock or Twilio)
        // Rate limiting is handled by RateLimitInterceptor
        String otp = otpService.sendOtp(phone);
        logger.info("OTP sent to {}", phone);
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        // Verify OTP using OtpService
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            throw new InvalidRequestException("OTP cannot be empty");
        }

        boolean isValid = otpService.verifyOtp(request.getPhone(), request.getOtp());
        if (!isValid) {
            throw new InvalidRequestException("Invalid or expired OTP");
        }

        // Find or create user
        Optional<User> userOpt = userRepository.findByPhone(request.getPhone());
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            // Mark as verified on successful OTP verification
            if (!user.getVerified()) {
                user.setVerified(true);
                userRepository.save(user);
            }
        } else {
            // Create new user with phone number as name (can be updated later)
            user = new User();
            user.setPhone(request.getPhone());
            user.setName(request.getPhone()); // Default name
            user.setVerified(true);
            user = userRepository.save(user);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getPhone());

        UserDto userDto = new UserDto(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getVerified(),
                user.getCreatedAt());

        return new AuthResponse(token, userDto);
    }
}
