// src/main/java/com/riskguard/entity/AffiliatePayoutRequest.java
package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "affiliate_payout_requests")
@Data
public class AffiliatePayoutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "affiliate_id")
    private Affiliate affiliate;

    private BigDecimal amount;
    private String upiId;
    private String status = "PENDING";

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime paidAt;
}