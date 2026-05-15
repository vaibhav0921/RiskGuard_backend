// src/main/java/com/riskguard/repository/AffiliatePayoutRepository.java
package com.riskguard.repository;

import com.riskguard.entity.AffiliatePayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AffiliatePayoutRepository extends JpaRepository<AffiliatePayoutRequest, Long> {
    List<AffiliatePayoutRequest> findByAffiliate_Id(Long affiliateId);
}