package com.gathr.controller;

import com.gathr.dto.AuthRequest;
import com.gathr.dto.AuthResponse;
import com.gathr.dto.OtpVerifyRequest;
import com.gathr.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/otp/start")
    public ResponseEntity<?> startOtp(@Valid @RequestBody AuthRequest request) {
        authService.startOtp(request);
        return ResponseEntity.ok().body(new MessageResponse("OTP sent successfully"));
    }
    
    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
    
    // Helper class for simple responses
    private record MessageResponse(String message) {}
}

