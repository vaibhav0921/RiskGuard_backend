// src/main/java/com/riskguard/entity/AffiliateReferral.java
package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "affiliate_referrals")
@Data
public class AffiliateReferral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "affiliate_id")
    private Affiliate affiliate;

    private String referredEmail;
    private String referralCode;

    private LocalDateTime clickedAt = LocalDateTime.now();

    private Boolean converted = false;
    private LocalDateTime convertedAt;

    private String planPurchased;
    private BigDecimal commissionAmount;
    private Boolean commissionPaid = false;
}