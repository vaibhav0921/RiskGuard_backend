package com.riskguard.service;

import com.riskguard.dto.*;
import com.riskguard.entity.*;
import com.riskguard.repository.*;
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
        Optional<User> opt = userRepo
                .findByEmailAndAccountNumber(email, account);

        if (opt.isEmpty()) {
            log.warn("[Validate] User not found: {} / {}", email, account);
            return new ValidateResponse(false, "NONE", "User not found", null);
        }

        User user = opt.get();

        boolean active = "ACTIVE".equalsIgnoreCase(user.getSubscriptionStatus());

        if (active && user.getExpiryDate() != null
                && user.getExpiryDate().isBefore(LocalDate.now())) {
            active = false;
            log.info("[Validate] Subscription expired for: {}", email);
        }

        String expiryDateStr = user.getExpiryDate() != null
                ? user.getExpiryDate().toString()
                : null;

        log.info("[Validate] {} / {} → active={} plan={}",
                email, account, active, user.getPlan());

        return new ValidateResponse(
                active,
                user.getPlan(),
                active ? "Subscription valid" : "Subscription inactive or expired",
                expiryDateStr
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
    public ValidateResponse register(RegisterRequest req) {
        String email   = req.getEmail();
        String account = req.getAccountNumber();
        String plan    = req.getPlan() != null
                ? req.getPlan().toUpperCase() : "BASIC";

        int months;
        switch (plan) {
            case "PRO":      months = 2; break;
            case "ADVANCED": months = 6; break;
            default:         months = 1; break;
        }

        User user = userRepo
                .findByEmailAndAccountNumber(email, account)
                .orElse(new User());

        user.setEmail(email);
        user.setAccountNumber(account);
        user.setSubscriptionStatus("ACTIVE");
        user.setPlan(plan);
        user.setExpiryDate(LocalDate.now().plusMonths(months));
        userRepo.save(user);

        if (rulesRepo.findByEmailAndAccountNumber(email, account).isEmpty()) {
            RiskRules rules = new RiskRules();
            rules.setEmail(email);
            rules.setAccountNumber(account);
            rules.setMaxDailyLoss(3.0);
            rules.setMaxTrades(5);
            rules.setMaxLossStreak(2);
            rulesRepo.save(rules);
        }

        log.info("[Register] User created/updated: {} / {} plan={} expiry={}",
                email, account, plan, user.getExpiryDate());

        return new ValidateResponse(
                true,
                plan,
                "Account activated successfully",
                user.getExpiryDate().toString()
        );
    }
}