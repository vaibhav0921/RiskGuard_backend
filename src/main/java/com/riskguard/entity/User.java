package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String accountNumber;

    // ACTIVE, INACTIVE, SUSPENDED
    @Column(nullable = false)
    private String subscriptionStatus = "INACTIVE";

    // FREE, BASIC, PRO
    @Column(nullable = false)
    private String plan = "FREE";

    @Column(name = "trial_used")
    private Boolean trialUsed = false;

    public Boolean getTrialUsed() { return trialUsed; }
    public void setTrialUsed(Boolean trialUsed) { this.trialUsed = trialUsed; }

    @Column
    private String paymentReference;

    private LocalDate expiryDate;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

}