package com.riskguard.repository;

import com.riskguard.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findByEmail(String email);

    /** Cleanup job — delete all expired, unverified OTPs older than 1 hour */
    @Modifying
    @Query("DELETE FROM EmailVerificationOtp o WHERE o.expiresAt < :cutoff AND o.verified = false")
    void deleteExpiredOtps(LocalDateTime cutoff);

    /** Check if email is already verified */
    boolean existsByEmailAndVerifiedTrue(String email);
}