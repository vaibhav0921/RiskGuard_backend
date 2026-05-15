// src/main/java/com/riskguard/dto/AffiliateDashboardResponse.java
package com.riskguard.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AffiliateDashboardResponse {
    private String referralCode;
    private String referralLink;
    private BigDecimal totalEarnings;
    private BigDecimal pendingEarnings;
    private BigDecimal paidEarnings;
    private Integer totalReferrals;
    private Integer totalConversions;
    private String upiId;
    private List<PayoutHistoryItem> payoutHistory;

    @Data
    public static class PayoutHistoryItem {
        private Long id;
        private BigDecimal amount;
        private String status;
        private String requestedAt;
        private String paidAt;
    }
}