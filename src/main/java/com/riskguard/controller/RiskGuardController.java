package com.riskguard.controller;

import com.riskguard.dto.*;
import com.riskguard.service.RiskGuardService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@CrossOrigin(origins = "http://localhost:3002")
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RiskGuardController {

    private final RiskGuardService service;

    //--- Seed test data on startup
//    @PostConstruct
//    public void init() {
//        service.seedTestUser();
//    }
    @GetMapping("/status")
    public ResponseEntity<StatusRequest> getStatus(
            @RequestParam String email,
            @RequestParam String account) {

        log.info("[API] GET /status — email={} account={}", email, account);
        return ResponseEntity.ok(service.getStatus(email, account));
    }
    //---------------------------------------------------------------
    // GET /api/validate?email=&account=
    //---------------------------------------------------------------
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestParam String email,
            @RequestParam String account) {

        log.info("[API] GET /validate — email={} account={}",
                email, account);
        return ResponseEntity.ok(service.validate(email, account));
    }

    //---------------------------------------------------------------
    // GET /api/rules?email=&account=
    //---------------------------------------------------------------
    @GetMapping("/rules")
    public ResponseEntity<RulesResponse> getRules(
            @RequestParam String email,
            @RequestParam String account) {

        log.info("[API] GET /rules — email={} account={}",
                email, account);
        return ResponseEntity.ok(service.getRules(email, account));
    }

    //---------------------------------------------------------------
    // POST /api/rules?email=&account=
    //---------------------------------------------------------------
    @PostMapping("/rules")
    public ResponseEntity<RulesResponse> updateRules(
            @RequestParam String email,
            @RequestParam String account,
            @RequestBody RulesResponse req) {

        log.info("[API] POST /rules — email={} account={} body={}",
                email, account, req);
        return ResponseEntity.ok(
                service.updateRules(email, account, req));
    }

    //---------------------------------------------------------------
    // POST /api/status
    //---------------------------------------------------------------
    @PostMapping("/status")
    public ResponseEntity<Void> receiveStatus(
            @RequestBody StatusRequest status) {

        log.info("[API] POST /status — from {}", status.getEmail());
        service.receiveStatus(status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download/RiskGuard.ex5")
    public ResponseEntity<Resource> downloadEA() throws IOException {
        Resource file = (Resource) new ClassPathResource("static/RiskGuard.ex5");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=RiskGuard.ex5")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    // Add this endpoint to RiskGuardController.java

    // POST /api/register
// Called by frontend after successful payment
    @PostMapping("/register")
    public ResponseEntity<ValidateResponse> register(
            @RequestBody RegisterRequest req) {

        log.info("[API] POST /register — email={} account={} plan={}",
                req.getEmail(), req.getAccountNumber(), req.getPlan());

        ValidateResponse response = service.register(req);
        return ResponseEntity.ok(response);
    }
}