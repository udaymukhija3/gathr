package com.gathr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "OTP is required")
    private String otp;
}

