package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores one OTP record per email.
 * One row per email — upserted on each send/resend.
 */
@Entity
@Table(name = "email_verification_otps",
        indexes = {
                @Index(name = "idx_otp_email", columnList = "email"),
                @Index(name = "idx_otp_expires_at", columnList = "expires_at")
        })
@Data
public class EmailVerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** true once /api/otp/verify succeeds */
    @Column(nullable = false)
    private boolean verified = false;

    /**
     * Number of failed verify attempts since last send.
     * Locked out after 5 wrong attempts.
     */
    @Column(nullable = false)
    private int attempts = 0;

    /**
     * Timestamp of the last time an OTP was sent/resent.
     * Used to enforce the 60-second resend cooldown.
     */
    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}