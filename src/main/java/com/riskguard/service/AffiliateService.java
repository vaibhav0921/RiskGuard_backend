// src/main/java/com/riskguard/service/AffiliateService.java
package com.riskguard.service;

import com.riskguard.dto.AffiliateDashboardResponse;
import com.riskguard.entity.*;
import com.riskguard.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliateService {

    private final AffiliateRepository affiliateRepo;
    private final AffiliateReferralRepository referralRepo;
    private final AffiliatePayoutRepository payoutRepo;

    private static final Map<String, BigDecimal> PLAN_COMMISSIONS = Map.of(
            "BASIC",    new BigDecimal("159.80"),
            "PRO",      new BigDecimal("279.80"),
            "ADVANCED", new BigDecimal("399.80"),
            "TRIAL",    BigDecimal.ZERO
    );

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder("RG-");
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    @Transactional
    public Affiliate joinAffiliate(String email, String name) {
        return affiliateRepo.findByEmail(email).orElseGet(() -> {
            Affiliate a = new Affiliate();
            a.setEmail(email);
            a.setName(name);
            String code;
            do { code = generateCode(); } while (affiliateRepo.findByReferralCode(code).isPresent());
            a.setReferralCode(code);
            affiliateRepo.save(a);
            log.info("[Affiliate] New affiliate: {} code={}", email, code);
            return a;
        });
    }

    public AffiliateDashboardResponse getDashboard(String email, String baseUrl) {
        Affiliate a = affiliateRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Not an affiliate"));

        List<AffiliatePayoutRequest> payouts = payoutRepo.findByAffiliate_Id(a.getId());

        AffiliateDashboardResponse resp = new AffiliateDashboardResponse();
        resp.setReferralCode(a.getReferralCode());
        resp.setReferralLink(baseUrl + "?ref=" + a.getReferralCode());
        resp.setTotalEarnings(a.getTotalEarnings());
        resp.setPendingEarnings(a.getPendingEarnings());
        resp.setPaidEarnings(a.getPaidEarnings());
        resp.setTotalReferrals(a.getTotalReferrals());
        resp.setTotalConversions(a.getTotalConversions());
        resp.setUpiId(a.getUpiId());

        List<AffiliateDashboardResponse.PayoutHistoryItem> history = new ArrayList<>();
        for (AffiliatePayoutRequest p : payouts) {
            AffiliateDashboardResponse.PayoutHistoryItem item = new AffiliateDashboardResponse.PayoutHistoryItem();
            item.setId(p.getId());
            item.setAmount(p.getAmount());
            item.setStatus(p.getStatus());
            item.setRequestedAt(p.getRequestedAt() != null ? p.getRequestedAt().toString() : null);
            item.setPaidAt(p.getPaidAt() != null ? p.getPaidAt().toString() : null);
            history.add(item);
        }
        resp.setPayoutHistory(history);
        return resp;
    }

    @Transactional
    public void trackClick(String referralCode) {
        affiliateRepo.findByReferralCode(referralCode).ifPresent(a -> {
            a.setTotalReferrals(a.getTotalReferrals() + 1);
            affiliateRepo.save(a);
        });
    }

    @Transactional
    public void recordConversion(String referralCode, String referredEmail, String plan) {
        if (referralCode == null || referralCode.isBlank()) return;

        affiliateRepo.findByReferralCode(referralCode).ifPresent(affiliate -> {
            // Skip if already converted for this email
            if (referralRepo.findByReferralCodeAndReferredEmail(referralCode, referredEmail).isPresent()) return;

            BigDecimal commission = PLAN_COMMISSIONS.getOrDefault(plan.toUpperCase(), BigDecimal.ZERO);

            AffiliateReferral ref = new AffiliateReferral();
            ref.setAffiliate(affiliate);
            ref.setReferredEmail(referredEmail);
            ref.setReferralCode(referralCode);
            ref.setConverted(true);
            ref.setConvertedAt(LocalDateTime.now());
            ref.setPlanPurchased(plan);
            ref.setCommissionAmount(commission);
            referralRepo.save(ref);

            affiliate.setTotalConversions(affiliate.getTotalConversions() + 1);
            affiliate.setTotalEarnings(affiliate.getTotalEarnings().add(commission));
            affiliate.setPendingEarnings(affiliate.getPendingEarnings().add(commission));
            affiliateRepo.save(affiliate);

            log.info("[Affiliate] Conversion: code={} email={} plan={} commission={}", referralCode, referredEmail, plan, commission);
        });
    }

    @Transactional
    public void requestPayout(String email, String upiId, BigDecimal amount) {
        Affiliate a = affiliateRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Not an affiliate"));

        if (a.getPendingEarnings().compareTo(new BigDecimal("500")) < 0)
            throw new IllegalStateException("Minimum payout is ₹500");

        if (amount.compareTo(a.getPendingEarnings()) > 0)
            throw new IllegalStateException("Amount exceeds pending earnings");

        a.setUpiId(upiId);

        AffiliatePayoutRequest req = new AffiliatePayoutRequest();
        req.setAffiliate(a);
        req.setAmount(amount);
        req.setUpiId(upiId);
        req.setStatus("PENDING");
        payoutRepo.save(req);

        a.setPendingEarnings(a.getPendingEarnings().subtract(amount));
        affiliateRepo.save(a);
        log.info("[Affiliate] Payout request: {} upi={} amount={}", email, upiId, amount);
    }

    @Transactional
    public void saveUpi(String email, String upiId) {
        affiliateRepo.findByEmail(email).ifPresent(a -> {
            a.setUpiId(upiId);
            affiliateRepo.save(a);
        });
    }

    public boolean isAffiliate(String email) {
        return affiliateRepo.findByEmail(email).isPresent();
    }
}