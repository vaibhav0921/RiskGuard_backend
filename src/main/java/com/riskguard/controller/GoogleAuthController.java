// src/main/java/com/riskguard/controller/GoogleAuthController.java
package com.riskguard.controller;

import com.riskguard.dto.ValidateResponse;
import com.riskguard.repository.UserRepository;
import com.riskguard.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final UserRepository userRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "idToken required"));

        try {
            // Verify token with Google
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = restTemplate.getForObject(url, Map.class);

            if (tokenInfo == null || tokenInfo.containsKey("error"))
                return ResponseEntity.status(401).body(Map.of("message", "Invalid Google token"));

            // Validate audience
            String aud = (String) tokenInfo.get("aud");
            if (googleClientId != null && !googleClientId.isBlank() && !googleClientId.equals(aud))
                return ResponseEntity.status(401).body(Map.of("message", "Token audience mismatch"));

            String email = (String) tokenInfo.get("email");
            String name  = (String) tokenInfo.get("name");

            if (email == null)
                return ResponseEntity.status(401).body(Map.of("message", "Could not extract email"));

            // Check if user exists with an active subscription
            var users = userRepo.findByEmail(email);
            boolean hasAccount = users.isPresent() && !users.get().isEmpty();
            boolean isNew = !hasAccount;

            // Check for active subscription
            String savedAccount = null;
            boolean isActive = false;
            if (hasAccount) {
                var activeUser = users.get().stream()
                        .filter(u -> "ACTIVE".equalsIgnoreCase(u.getSubscriptionStatus()))
                        .findFirst();
                if (activeUser.isPresent()) {
                    User u = activeUser.get();
                    isActive = u.getExpiryDate() == null || !u.getExpiryDate().isBefore(LocalDate.now());
                    savedAccount = isActive ? u.getAccountNumber() : null;
                }
            }

            log.info("[GoogleAuth] email={} isNew={} hasAccount={} isActive={}", email, isNew, hasAccount, isActive);

            return ResponseEntity.ok(Map.of(
                    "email", email,
                    "name",  name != null ? name : "",
                    "isNewUser",   isNew,
                    "hasAccount",  hasAccount,
                    "isActive",    isActive,
                    "savedAccount", savedAccount != null ? savedAccount : ""
            ));

        } catch (Exception e) {
            log.error("[GoogleAuth] Verification failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "Token verification failed"));
        }
    }

    @PostMapping("/save-account")
    public ResponseEntity<?> saveAccount(@RequestBody Map<String, String> body) {
        String email   = body.get("email");
        String account = body.get("account");
        if (email == null || account == null)
            return ResponseEntity.badRequest().body(Map.of("message", "email and account required"));

        // This just validates — actual account is saved on /api/register (post-payment)
        // For existing users checking their account number
        var users = userRepo.findByEmail(email);
        if (users.isPresent()) {
            var match = users.get().stream()
                    .filter(u -> u.getAccountNumber().equals(account))
                    .findFirst();
            if (match.isPresent()) {
                User u = match.get();
                boolean active = "ACTIVE".equalsIgnoreCase(u.getSubscriptionStatus())
                        && (u.getExpiryDate() == null || !u.getExpiryDate().isBefore(LocalDate.now()));
                return ResponseEntity.ok(Map.of(
                        "active", active,
                        "plan", u.getPlan(),
                        "expiryDate", u.getExpiryDate() != null ? u.getExpiryDate().toString() : ""
                ));
            }
        }
        return ResponseEntity.ok(Map.of("active", false, "plan", "NONE", "expiryDate", ""));
    }
}