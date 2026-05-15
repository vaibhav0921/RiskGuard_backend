// src/main/java/com/riskguard/dto/RegisterRequest.java
package com.riskguard.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String accountNumber;
    private String plan;
    private String paymentRef;
    private String referralCode;  // NEW
}