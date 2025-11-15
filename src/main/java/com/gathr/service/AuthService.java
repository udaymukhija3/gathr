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

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // Mock OTP storage (in production, use Redis or database)
    private static final String MOCK_OTP = "123456";

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public void startOtp(AuthRequest request) {
        // Mock OTP sending - in production, integrate with SMS service
        // For now, we just log it. The OTP is always "123456" for testing
        logger.info("Mock: Sending OTP {} to {}", MOCK_OTP, request.getPhone());
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        // Mock OTP verification - accept any non-empty OTP for testing
        // In production, verify against stored OTP with expiry
        if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
            throw new InvalidRequestException("Invalid OTP");
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
            user.getCreatedAt()
        );
        
        return new AuthResponse(token, userDto);
    }
}

