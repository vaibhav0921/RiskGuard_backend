// src/main/java/com/riskguard/controller/SseController.java
package com.riskguard.controller;

import com.riskguard.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * Frontend connects here once on mount.
     * GET /api/trades/stream?account=12345678
     *
     * Returns a persistent SSE stream. The browser's native EventSource
     * API keeps this connection open and automatically reconnects if it drops.
     *
     * Important: produces TEXT_EVENT_STREAM_VALUE — this is what tells
     * Spring to keep the connection open instead of closing it after the response.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String account) {
        return sseService.connect(account);
    }

    /**
     * Sends a heartbeat every 30 seconds to all connected clients.
     * Prevents proxies and load balancers from closing idle connections.
     * Requires @EnableScheduling in your Spring Boot app or a config class.
     */
    @Scheduled(fixedDelay = 30_000)
    public void heartbeat() {
        sseService.sendHeartbeat();
    }
}