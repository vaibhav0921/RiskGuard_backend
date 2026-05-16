// src/main/java/com/riskguard/entity/Trade.java
package com.riskguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Long ticket;          // MT5 deal ticket

    private String symbol;
    private String type;          // BUY | SELL
    private Double volume;
    private Double openPrice;
    private Double closePrice;
    private Double profit;

    // EA_STOP_LOSS | EA_DAILY_LOSS_LIMIT | EA_CONSECUTIVE_LOSS |
    // EA_MAX_TRADES | EA_RULE_CLOSE | SL_HIT | TP_HIT | MANUAL
    private String closeReason;

    // JSON array stored as text e.g. [] or ["DAILY_LOSS_LIMIT","CONSECUTIVE_LOSS"]
    @Column(columnDefinition = "TEXT")
    private String rulesBroken;

    private LocalDateTime openTime;
    private LocalDateTime closeTime;

    @Column(nullable = false)
    private LocalDate tradeDate;   // derived from closeTime, used for daily grouping

    // true if rulesBroken is non-empty — denormalised for fast daily queries
    @Column(nullable = false)
    private Boolean disciplineBreached;
}