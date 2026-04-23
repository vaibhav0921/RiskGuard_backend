package com.riskguard.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.riskguard.dto.*;
import com.riskguard.service.RiskGuardService;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;


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

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) throws Exception {

        int amount = (int) data.get("amount"); // in paise

        RazorpayClient client = new RazorpayClient("rzp_test_SfitmtfFH9SX9K", "2aeDNLvK9gxtNPhNfe7BCkZB");

        JSONObject options = new JSONObject();
        options.put("amount", amount);
        options.put("currency", "INR");
        options.put("receipt", "order_" + System.currentTimeMillis());

        Order order = client.orders.create(options);

        return ResponseEntity.ok(order.toString());
    }


    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) throws Exception {

        String orderId = data.get("razorpay_order_id");
        String paymentId = data.get("razorpay_payment_id");
        String signature = data.get("razorpay_signature");

        String payload = orderId + "|" + paymentId;

        String expectedSignature = Utils.getHash(payload, "2aeDNLvK9gxtNPhNfe7BCkZB");

        if (expectedSignature.equals(signature)) {

            // ✅ PAYMENT SUCCESS → Activate subscription
            // TODO: call your subscription logic here

            return ResponseEntity.ok("Payment verified");
        }

        return ResponseEntity.status(400).body("Invalid payment");
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
    public ResponseEntity<byte[]> downloadEA() {
        try {
            ClassPathResource resource = new ClassPathResource("static/risk_app.ex5");

            if (!resource.exists()) {
                log.error("[Download] risk_app.ex5 not found");
                return ResponseEntity.notFound().build();
            }

            byte[] fileBytes = resource.getInputStream().readAllBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "risk_app.ex5");
            headers.setContentLength(fileBytes.length);

            log.info("[Download] Serving RiskGuard.ex5 ({} bytes)", fileBytes.length);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("[Download] Failed to read EA file: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
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