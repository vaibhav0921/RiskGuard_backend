// src/main/java/com/riskguard/entity/EaStatus.java
package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ea_status")
@Data
public class EaStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String  email;
    private String  accountNumber;
    private boolean tradingAllowed;
    private boolean licenseActive;
    private int     consecutiveLosses;
    private int     tradesToday;
    private double  dailyLossPercent;
    private double  currentEquity;
    private String  disabledReason;
    private boolean isEAConnected ;

    // Updated every time EA posts
    private LocalDateTime lastUpdated;
}