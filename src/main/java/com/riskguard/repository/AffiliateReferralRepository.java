// src/main/java/com/riskguard/repository/AffiliateReferralRepository.java
package com.riskguard.repository;

import com.riskguard.entity.AffiliateReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AffiliateReferralRepository extends JpaRepository<AffiliateReferral, Long> {
    List<AffiliateReferral> findByAffiliate_Id(Long affiliateId);
    Optional<AffiliateReferral> findByReferralCodeAndReferredEmail(String code, String email);
}