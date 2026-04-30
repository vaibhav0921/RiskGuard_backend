package com.riskguard.service;

import com.riskguard.dto.ValidateResponse;
import com.riskguard.entity.RiskRules;
import com.riskguard.entity.User;
import com.riskguard.repository.RiskRulesRepository;
import com.riskguard.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialService {

    private final UserRepository      userRepo;
    private final RiskRulesRepository rulesRepo;

    // Returns true only if this MT5 account has NEVER had a trial
    public boolean isEligible(String account) {
        Optional<User> existing = userRepo.findByAccountNumber(account);
        if (existing.isEmpty()) return true;                  // brand new account
        User user = existing.get();
        // If they had a trial before, not eligible again
        return !"TRIAL".equalsIgnoreCase(user.getPlan())
                && user.getTrialUsed() == null || !user.getTrialUsed();
    }

    @Transactional
    public ValidateResponse activateTrial(String email, String account) {

        // Block if MT5 account belongs to a different email
        Optional<User> accountTaken = userRepo.findByAccountNumber(account);
        if (accountTaken.isPresent() &&
                !accountTaken.get().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalStateException(
                    "This MT5 account is already registered with a different email.");
        }

        // Block if trial already used for this MT5 account
        if (accountTaken.isPresent()) {
            User existing = accountTaken.get();
            if (Boolean.TRUE.equals(existing.getTrialUsed())) {
                throw new IllegalStateException(
                        "Free trial has already been used for this MT5 account.");
            }
        }

        User user = accountTaken
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .orElse(new User());

        user.setEmail(email);
        user.setAccountNumber(account);
        user.setSubscriptionStatus("ACTIVE");
        user.setPlan("TRIAL");
        user.setExpiryDate(LocalDate.now().plusDays(7));
        user.setTrialUsed(true);
        user.setPaymentReference("TRIAL_FREE");
        if (user.getCreatedAt() == null)
            user.setCreatedAt(LocalDateTime.now());

        userRepo.save(user);

        // Create default rules if not exist
        if (rulesRepo.findByEmailAndAccountNumber(email, account).isEmpty()) {
            RiskRules rules = new RiskRules();
            rules.setEmail(email);
            rules.setAccountNumber(account);
            rules.setMaxDailyLoss(3.0);
            rules.setMaxTrades(5);
            rules.setMaxLossStreak(2);
            rules.setResetTime("21:30");
            rulesRepo.save(rules);
        }

        log.info("[Trial] Activated for {} / {} — expires {}",
                email, account, user.getExpiryDate());

        return new ValidateResponse(
                true, "TRIAL",
                "Free trial activated! Enjoy 7 days of RiskGuard.",
                user.getExpiryDate().toString()
        );
    }
}