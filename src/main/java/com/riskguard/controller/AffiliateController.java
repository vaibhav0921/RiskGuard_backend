// src/main/java/com/riskguard/controller/AffiliateController.java
package com.riskguard.controller;

import com.riskguard.dto.AffiliateDashboardResponse;
import com.riskguard.entity.Affiliate;
import com.riskguard.service.AffiliateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/affiliate")
@RequiredArgsConstructor
public class AffiliateController {

    private final AffiliateService affiliateService;

    private static final String FRONTEND_URL = "http://localhost:3001";
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String name  = body.get("name");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("message", "Email required"));
        try {
            Affiliate a = affiliateService.joinAffiliate(email, name);
            return ResponseEntity.ok(Map.of(
                    "referralCode", a.getReferralCode(),
                    "message", "Welcome to the affiliate program!"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestParam String email) {
        try {
            if (!affiliateService.isAffiliate(email))
                return ResponseEntity.status(404).body(Map.of("message", "Not an affiliate"));
            AffiliateDashboardResponse dashboard = affiliateService.getDashboard(email, FRONTEND_URL);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/track-click")
    public ResponseEntity<?> trackClick(@RequestBody Map<String, String> body) {
        String code = body.get("referralCode");
        if (code != null) affiliateService.trackClick(code);
        return ResponseEntity.ok(Map.of("tracked", true));
    }

    @PostMapping("/request-payout")
    public ResponseEntity<?> requestPayout(@RequestBody Map<String, Object> body) {
        String email  = (String) body.get("email");
        String upiId  = (String) body.get("upiId");
        Object amtObj = body.get("amount");
        if (email == null || upiId == null || amtObj == null)
            return ResponseEntity.badRequest().body(Map.of("message", "email, upiId and amount are required"));
        try {
            BigDecimal amount = new BigDecimal(amtObj.toString());
            affiliateService.requestPayout(email, upiId, amount);
            return ResponseEntity.ok(Map.of("message", "Payout request submitted!"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/save-upi")
    public ResponseEntity<?> saveUpi(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String upiId = body.get("upiId");
        if (email == null || upiId == null) return ResponseEntity.badRequest().body(Map.of("message", "Required"));
        affiliateService.saveUpi(email, upiId);
        return ResponseEntity.ok(Map.of("message", "UPI saved"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("isAffiliate", affiliateService.isAffiliate(email)));
    }
}