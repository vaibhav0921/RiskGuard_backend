// src/main/java/com/riskguard/controller/TradeController.java
package com.riskguard.controller;

import com.riskguard.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // Called by EA when a trade closes
    // POST /api/trades/close
    @PostMapping("/close")
    public ResponseEntity<?> recordTrade(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = tradeService.recordTrade(body);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[Trade] Failed to record trade: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to record trade."));
        }
    }

    // Called by frontend to load history
    // GET /api/trades/history?account=12345678&days=30
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam String account,
            @RequestParam(defaultValue = "30") int days) {
        try {
            return ResponseEntity.ok(tradeService.getHistory(account, days));
        } catch (Exception e) {
            log.error("[Trade] Failed to fetch history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to fetch history."));
        }
    }
}