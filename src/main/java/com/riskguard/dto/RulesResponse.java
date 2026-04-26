// RulesResponse.java
package com.riskguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RulesResponse {
    private double maxDailyLoss;
    private int    maxTrades;
    private int    maxLossStreak;
    private String resetTime;
}