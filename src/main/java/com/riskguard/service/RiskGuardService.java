package com.riskguard.service;

import com.riskguard.dto.*;
import com.riskguard.entity.*;
import com.riskguard.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskGuardService {

    private final UserRepository      userRepo;
    private final RiskRulesRepository rulesRepo;

    //---------------------------------------------------------------
    // VALIDATE LICENSE
    //---------------------------------------------------------------
    public ValidateResponse validate(String email, String account) {

        // Case 1 — Exact match found
        Optional<User> exactMatch = userRepo
                .findByEmailAndAccountNumber(email, account);

        if (exactMatch.isPresent()) {
            User user = exactMatch.get();

            boolean active = "ACTIVE".equalsIgnoreCase(
                    user.getSubscriptionStatus());

            // Auto expire
            if (active && user.getExpiryDate() != null
                    && user.getExpiryDate().isBefore(LocalDate.now())) {
                active = false;
                user.setSubscriptionStatus("EXPIRED");
                userRepo.save(user);
                log.info("[Validate] Expired: {} / {}", email, account);
            }

            String expiry = user.getExpiryDate() != null
                    ? user.getExpiryDate().toString() : null;

            log.info("[Validate] {} / {} → active={} plan={}",
                    email, account, active, user.getPlan());

            return new ValidateResponse(
                    active,
                    user.getPlan(),
                    active ? "Subscription valid"
                            : "Subscription inactive or expired",
                    expiry
            );
        }

        // Case 2 — This MT5 account belongs to a different email
        // One MT5 account can only have ONE owner
        Optional<User> accountTaken = userRepo.findByAccountNumber(account);
        if (accountTaken.isPresent()) {
            log.warn("[Validate] Account {} already owned by different email",
                    account);
            return new ValidateResponse(
                    false, "NONE",
                    "This MT5 account number is already registered " +
                            "with a different email address.",
                    null
            );
        }

        // Case 3 — Email exists but this account is not registered yet
        // Same user with a new/different MT5 account — send to plans
        Optional<User> emailExists = userRepo.findByEmail(email);
        if (emailExists.isPresent()) {
            log.info("[Validate] Known email {} with new account {} → plans",
                    email, account);
            return new ValidateResponse(
                    false, "NEW_ACCOUNT",
                    "This MT5 account is not registered yet. " +
                            "Please subscribe to activate it.",
                    null
            );
        }

        // Case 4 — Completely new user
        log.info("[Validate] New user {} / {} → plans", email, account);
        return new ValidateResponse(
                false, "NONE",
                "No account found. Please complete payment to register.",
                null
        );
    }

    //---------------------------------------------------------------
    // FETCH RULES
    //---------------------------------------------------------------
    public RulesResponse getRules(String email, String account) {
        Optional<RiskRules> opt = rulesRepo
                .findByEmailAndAccountNumber(email, account);

        if (opt.isEmpty()) {
            log.info("[Rules] No rules found for {} — using defaults", email);
            return new RulesResponse(3.0, 5, 2);
        }

        RiskRules rules = opt.get();
        log.info("[Rules] Fetched for {} — loss={}% trades={} streak={}",
                email, rules.getMaxDailyLoss(),
                rules.getMaxTrades(), rules.getMaxLossStreak());

        return new RulesResponse(
                rules.getMaxDailyLoss(),
                rules.getMaxTrades(),
                rules.getMaxLossStreak());
    }

    //---------------------------------------------------------------
    // UPDATE RULES
    //---------------------------------------------------------------
    public RulesResponse updateRules(String email,
                                     String account,
                                     RulesResponse req) {
        RiskRules rules = rulesRepo
                .findByEmailAndAccountNumber(email, account)
                .orElse(new RiskRules());

        rules.setEmail(email);
        rules.setAccountNumber(account);
        rules.setMaxDailyLoss(req.getMaxDailyLoss());
        rules.setMaxTrades(req.getMaxTrades());
        rules.setMaxLossStreak(req.getMaxLossStreak());

        rulesRepo.save(rules);
        log.info("[Rules] Updated for {} / {}", email, account);

        return req;
    }

    //---------------------------------------------------------------
    // SEED TEST DATA
    //---------------------------------------------------------------
    public void seedTestUser() {
        if (userRepo.count() > 0) return;

        User user = new User();
        user.setEmail("vaibhavnanavare600@gmail.com");
        user.setAccountNumber("1301259166");
        user.setSubscriptionStatus("ACTIVE");
        user.setPlan("PRO");
        user.setExpiryDate(LocalDate.now().plusYears(1));
        userRepo.save(user);

        RiskRules rules = new RiskRules();
        rules.setEmail("vaibhavnanavare600@gmail.com");
        rules.setAccountNumber("1301259166");
        rules.setMaxDailyLoss(3.0);
        rules.setMaxTrades(5);
        rules.setMaxLossStreak(2);
        rulesRepo.save(rules);

        log.info("[Seed] Test user and rules created.");
    }

    @Autowired
    private EaStatusRepository statusRepo;

    //---------------------------------------------------------------
    // RECEIVE STATUS FROM EA  (POST /api/status)
    //---------------------------------------------------------------
    public void receiveStatus(StatusRequest req) {
        EaStatus status = statusRepo
                .findByEmailAndAccountNumber(req.getEmail(), req.getAccountNumber())
                .orElse(new EaStatus());

        status.setEmail(req.getEmail());
        status.setAccountNumber(req.getAccountNumber());
        status.setTradingAllowed(req.isTradingAllowed());
        status.setLicenseActive(req.isLicenseActive());
        status.setConsecutiveLosses(req.getConsecutiveLosses());
        status.setTradesToday(req.getTradesToday());
        status.setDailyLossPercent(req.getDailyLossPercent());
        status.setCurrentEquity(req.getCurrentEquity());
        status.setDisabledReason(req.getDisabledReason());
        status.setLastUpdated(LocalDateTime.now());

        // FIX 1: Save startOfDayEquity sent by EA.
        // Previously this field was received in StatusRequest but never
        // persisted — so getStatus() always returned null for it,
        // causing the frontend to fall back to currentEquity for P&L
        // calculation which gave wrong dollar amounts.
        if (req.getStartOfDayEquity() != null && req.getStartOfDayEquity() > 0) {
            status.setStartOfDayEquity(req.getStartOfDayEquity());
        }

        // FIX 2: eaConnected must be true only when real equity data exists.
        // Old code: status.setEaConnected(req.isLicenseActive())
        // That would mark EA as connected even if equity=0 (EA just started,
        // no real data yet), causing dashboard to show 0 values as "Active".
        boolean hasRealData = req.getCurrentEquity() > 0;
        status.setEaConnected(hasRealData);

        statusRepo.save(status);

        log.info("[Status] Saved — equity={} startOfDay={} trades={} loss={}% trading={} reason={}",
                req.getCurrentEquity(),
                req.getStartOfDayEquity(),
                req.getTradesToday(),
                req.getDailyLossPercent(),
                req.isTradingAllowed(),
                req.getDisabledReason());
    }

    //---------------------------------------------------------------
    // GET STATUS FOR FRONTEND  (GET /api/status)
    //---------------------------------------------------------------
    public StatusRequest getStatus(String email, String account) {
        EaStatus status = statusRepo
                .findByEmailAndAccountNumber(email, account)
                .orElse(null);

        if (status == null) {
            // EA has never connected — return disconnected state.
            // Frontend shows "Waiting for EA connection" screen.
            StatusRequest empty = new StatusRequest();
            empty.setEmail(email);
            empty.setAccountNumber(account);
            empty.setTradingAllowed(true);
            empty.setCurrentEquity(0);
            empty.setStartOfDayEquity(0.0);
            empty.setTradesToday(0);
            empty.setDailyLossPercent(0);
            empty.setConsecutiveLosses(0);
            empty.setDisabledReason("");
            empty.setEaConnected(false);
            return empty;
        }

        StatusRequest res = new StatusRequest();
        res.setEmail(status.getEmail());
        res.setAccountNumber(status.getAccountNumber());
        res.setTradingAllowed(status.isTradingAllowed());
        res.setLicenseActive(status.isLicenseActive());
        res.setConsecutiveLosses(status.getConsecutiveLosses());
        res.setTradesToday(status.getTradesToday());
        res.setDailyLossPercent(status.getDailyLossPercent());
        res.setCurrentEquity(status.getCurrentEquity());
        res.setDisabledReason(status.getDisabledReason());

        // FIX 3: Return startOfDayEquity to frontend.
        // Frontend uses: pnlAmt = currentEquity - startOfDayEquity
        // Without this, frontend falls back to currentEquity as baseline
        // which gives wrong P&L numbers (showed +$4 when actually -$6).
        res.setStartOfDayEquity(status.getStartOfDayEquity());

        // FIX 2 (read side): use stored eaConnected flag, not licenseActive.
        res.setEaConnected(status.isEaConnected());

        log.info("[Status] Returned to frontend — equity={} startOfDay={} eaConnected={}",
                res.getCurrentEquity(),
                res.getStartOfDayEquity(),
                res.isEaConnected());

        return res;
    }

    //---------------------------------------------------------------
    // REGISTER USER AFTER PAYMENT
    //---------------------------------------------------------------
    @Transactional
    public ValidateResponse register(RegisterRequest req) {
        String email   = req.getEmail();
        String account = req.getAccountNumber();
        String plan    = req.getPlan() != null
                ? req.getPlan().toUpperCase() : "BASIC";

        // Block only if MT5 account is taken by a DIFFERENT email
        Optional<User> accountTaken = userRepo.findByAccountNumber(account);
        if (accountTaken.isPresent() &&
                !accountTaken.get().getEmail().equalsIgnoreCase(email)) {
            log.warn("[Register] Account {} already owned by {}",
                    account, accountTaken.get().getEmail());
            throw new RuntimeException(
                    "This MT5 account is already registered with a different email.");
        }

        // Same email + same account = renewal or upgrade, allow it
        int months = switch (plan) {
            case "PRO"      -> 2;
            case "ADVANCED" -> 6;
            default         -> 1;
        };

        User user = userRepo
                .findByEmailAndAccountNumber(email, account)
                .orElse(new User());

        user.setEmail(email);
        user.setAccountNumber(account);
        user.setSubscriptionStatus("ACTIVE");
        user.setPlan(plan);
        user.setExpiryDate(LocalDate.now().plusMonths(months));
        user.setPaymentReference(req.getPaymentRef());
        if (user.getCreatedAt() == null)
            user.setCreatedAt(LocalDateTime.now());
        userRepo.save(user);

        // Create default rules for this account if not exist
        if (rulesRepo.findByEmailAndAccountNumber(email, account).isEmpty()) {
            RiskRules rules = new RiskRules();
            rules.setEmail(email);
            rules.setAccountNumber(account);
            rules.setMaxDailyLoss(3.0);
            rules.setMaxTrades(5);
            rules.setMaxLossStreak(2);
            rulesRepo.save(rules);
        }

        log.info("[Register] {} / {} plan={} expiry={}",
                email, account, plan, user.getExpiryDate());

        return new ValidateResponse(
                true, plan,
                "Account activated successfully",
                user.getExpiryDate().toString()
        );
    }
}