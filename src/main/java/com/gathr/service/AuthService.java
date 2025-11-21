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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    // Rate limiting: max 3 requests per hour per phone
    // In-memory storage (use Redis in production)
    private final Map<String, OtpRequestRecord> otpRequestHistory = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final int RATE_LIMIT_WINDOW_HOURS = 1;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, OtpService otpService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
    }

    public void startOtp(AuthRequest request) {
        String phone = request.getPhone();
        
        // Rate limiting check
        OtpRequestRecord record = otpRequestHistory.get(phone);
        LocalDateTime now = LocalDateTime.now();
        
        if (record != null) {
            // Remove old requests outside the window
            record.removeOldRequests(now.minusHours(RATE_LIMIT_WINDOW_HOURS));
            
            if (record.getRequestCount() >= MAX_REQUESTS_PER_HOUR) {
                logger.warn("Rate limit exceeded for phone: {}", phone);
                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again later."
                );
            }
            
            record.addRequest(now);
        } else {
            record = new OtpRequestRecord();
            record.addRequest(now);
            otpRequestHistory.put(phone, record);
        }

        // Send OTP via configured OtpService (mock or Twilio)
        String otp = otpService.sendOtp(phone);
        logger.info("OTP sent to {}", phone);
    }

    // Helper class for rate limiting
    private static class OtpRequestRecord {
        private final java.util.List<LocalDateTime> requests = new java.util.ArrayList<>();

        public void addRequest(LocalDateTime timestamp) {
            requests.add(timestamp);
        }

        public void removeOldRequests(LocalDateTime threshold) {
            requests.removeIf(timestamp -> timestamp.isBefore(threshold));
        }

        public int getRequestCount() {
            return requests.size();
        }
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
            user.getCreatedAt()
        );
        
        return new AuthResponse(token, userDto);
    }
}

