package com.riskguard.controller;

import com.riskguard.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    // POST /api/auth/send-otp
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        try {
            otpService.sendOtp(email.trim().toLowerCase());
            log.info("[OTP] Sent to {}", email);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully."));
        } catch (Exception e) {
            log.error("[OTP] Failed to send to {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to send OTP. Please try again."));
        }
    }

    // POST /api/auth/verify-otp
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");
        if (email == null || otp == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required."));

        boolean valid = otpService.verifyOtp(email.trim().toLowerCase(), otp.trim());
        if (valid) {
            log.info("[OTP] Verified for {}", email);
            return ResponseEntity.ok(Map.of("verified", true));
        }
        log.warn("[OTP] Invalid/expired for {}", email);
        return ResponseEntity.status(401)
                .body(Map.of("verified", false, "message", "Invalid or expired OTP."));
    }
}