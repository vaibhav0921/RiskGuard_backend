// src/main/java/com/riskguard/entity/Affiliate.java
package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "affiliates")
@Data
public class Affiliate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(nullable = false, unique = true)
    private String referralCode;

    private String upiId;

    private BigDecimal totalEarnings = BigDecimal.ZERO;
    private BigDecimal pendingEarnings = BigDecimal.ZERO;
    private BigDecimal paidEarnings = BigDecimal.ZERO;

    private Integer totalReferrals = 0;
    private Integer totalConversions = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
}