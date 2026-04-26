package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "risk_rules")
@Data
public class RiskRules {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String accountNumber;

    private double maxDailyLoss   = 3.0;   // percent
    private int    maxTrades      = 5;
    private int    maxLossStreak  = 2;
    private String resetTime ="21:30";
}