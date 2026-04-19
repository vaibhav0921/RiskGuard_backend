// ValidateResponse.java
package com.riskguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidateResponse {
    private boolean active;
    private String  plan;
    private String  message;
    private String expiryDate; // ISO date string from User.expiryDate
}