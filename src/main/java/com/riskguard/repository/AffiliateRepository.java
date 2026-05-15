// src/main/java/com/riskguard/repository/AffiliateRepository.java
package com.riskguard.repository;

import com.riskguard.entity.Affiliate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AffiliateRepository extends JpaRepository<Affiliate, Long> {
    Optional<Affiliate> findByEmail(String email);
    Optional<Affiliate> findByReferralCode(String referralCode);
}