package com.riskguard.controller;

import com.riskguard.dto.ValidateResponse;
import com.riskguard.service.TrialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trial")
@RequiredArgsConstructor
public class TrialController {

    private final TrialService trialService;

    // GET /api/trial/eligibility?account=
    @GetMapping("/eligibility")
    public ResponseEntity<?> checkEligibility(@RequestParam String account) {
        boolean eligible = trialService.isEligible(account);
        log.info("[Trial] Eligibility check — account={} eligible={}", account, eligible);
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    // POST /api/trial/activate
    @PostMapping("/activate")
    public ResponseEntity<?> activateTrial(@RequestBody Map<String, String> body) {
        String email   = body.get("email");
        String account = body.get("account");

        if (email == null || account == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email and account are required."));
        }

        try {
            ValidateResponse res = trialService.activateTrial(email.trim(), account.trim());
            log.info("[Trial] Activated — email={} account={}", email, account);
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            log.warn("[Trial] Rejected — {}", e.getMessage());
            return ResponseEntity.status(409)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}