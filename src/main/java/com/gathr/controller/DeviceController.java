package com.gathr.controller;

import com.gathr.entity.UserDevice;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.UserDeviceRepository;
import com.gathr.repository.UserRepository;
import com.gathr.security.AuthenticatedUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for managing user device tokens for push notifications.
 */
@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final UserDeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public DeviceController(
            UserDeviceRepository deviceRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<DeviceResponse> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check if device token already exists
        UserDevice device = deviceRepository.findByDeviceToken(request.deviceToken())
                .orElse(null);

        if (device != null) {
            // Update existing device
            device.setUser(user);
            device.setDeviceType(request.deviceType());
            device.setDeviceName(request.deviceName());
            device.setIsActive(true);
            device.setLastUsedAt(LocalDateTime.now());
        } else {
            // Create new device
            device = new UserDevice();
            device.setUser(user);
            device.setDeviceToken(request.deviceToken());
            device.setDeviceType(request.deviceType());
            device.setDeviceName(request.deviceName());
            device.setIsActive(true);
            device.setLastUsedAt(LocalDateTime.now());
        }

        device = deviceRepository.save(device);

        return ResponseEntity.ok(new DeviceResponse(
                device.getId(),
                device.getDeviceType().name(),
                device.getDeviceName(),
                device.getIsActive()
        ));
    }

    @DeleteMapping("/{deviceId}")
    @Transactional
    public ResponseEntity<Void> unregisterDevice(
            @PathVariable Long deviceId,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        deviceRepository.findById(deviceId).ifPresent(device -> {
            if (device.getUser().getId().equals(userId)) {
                device.setIsActive(false);
                deviceRepository.save(device);
            }
        });

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getMyDevices(Authentication authentication) {
        Long userId = authenticatedUserService.requireUserId(authentication);

        List<DeviceResponse> devices = deviceRepository.findByUserId(userId).stream()
                .map(d -> new DeviceResponse(
                        d.getId(),
                        d.getDeviceType().name(),
                        d.getDeviceName(),
                        d.getIsActive()
                ))
                .toList();

        return ResponseEntity.ok(devices);
    }

    public record RegisterDeviceRequest(
            @NotBlank String deviceToken,
            @NotNull UserDevice.DeviceType deviceType,
            String deviceName
    ) {}

    public record DeviceResponse(
            Long id,
            String deviceType,
            String deviceName,
            Boolean isActive
    ) {}
}
