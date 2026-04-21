// StatusRequest.java
package com.riskguard.dto;

import lombok.Data;

@Data
public class StatusRequest {
    private String  email;
    private String  accountNumber;
    private boolean tradingAllowed;
    private boolean licenseActive;
    private int     consecutiveLosses;
    private int     tradesToday;
    private double  dailyLossPercent;
    private double  currentEquity;
    private String  disabledReason;
    private boolean eaConnected;
    private Double startOfDayEquity;
}