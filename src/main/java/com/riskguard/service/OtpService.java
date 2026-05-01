package com.riskguard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;

    private static class OtpEntry {
        final String otp;
        final Instant expiresAt;
        OtpEntry(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final int EXPIRY_SECONDS = 300; // 5 minutes
    private static final SecureRandom random = new SecureRandom();

    public void sendOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(EXPIRY_SECONDS)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Your RiskGuard verification code");
        msg.setText(
                "Hello,\n\n" +
                        "Your RiskGuard verification code is:\n\n" +
                        "  " + otp + "\n\n" +
                        "This code expires in 5 minutes.\n" +
                        "If you didn't request this, please ignore this email.\n\n" +
                        "— RiskGuard Team"
        );
        mailSender.send(msg);
        log.info("[OTP] Generated and sent to {}", email);
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt)) {
            otpStore.remove(email);
            return false;
        }
        if (entry.otp.equals(otp)) {
            otpStore.remove(email); // single use
            return true;
        }
        return false;
    }
}