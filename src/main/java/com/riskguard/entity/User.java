package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

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

    private LocalDate expiryDate;
}