// src/main/java/com/riskguard/service/SseService.java
package com.riskguard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    // account → emitter — one live connection per account
    // ConcurrentHashMap so add/remove from different threads is safe
    private final Map<String, SseEmitter> clients = new ConcurrentHashMap<>();

    /**
     * Called by SseController when a frontend tab connects.
     * Creates an emitter that stays open for up to 5 minutes,
     * then the browser automatically reconnects (EventSource handles this).
     */
    public SseEmitter connect(String account) {
        // If there is already a connection for this account, complete it first
        SseEmitter existing = clients.get(account);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 min timeout

        // Clean up when the connection closes for any reason
        emitter.onCompletion(() -> {
            clients.remove(account);
            log.info("[SSE] Client disconnected — account={}", account);
        });
        emitter.onTimeout(() -> {
            clients.remove(account);
            log.info("[SSE] Client timed out — account={} (browser will reconnect)", account);
        });
        emitter.onError(ex -> {
            clients.remove(account);
            log.warn("[SSE] Client error — account={} reason={}", account, ex.getMessage());
        });

        clients.put(account, emitter);
        log.info("[SSE] Client connected — account={} | total clients={}", account, clients.size());

        // Send an immediate "connected" heartbeat so the browser knows the stream is live
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("ok"));
        } catch (IOException e) {
            log.warn("[SSE] Could not send initial heartbeat to account={}", account);
        }

        return emitter;
    }

    /**
     * Called by TradeService after a trade is saved.
     * Pushes a "trade" event to the specific account's frontend tab.
     * The frontend receives this and immediately refetches history.
     */
    public void pushTradeEvent(String account) {
        SseEmitter emitter = clients.get(account);
        if (emitter == null) {
            log.debug("[SSE] No connected client for account={} — skipping push", account);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("trade")       // frontend listens for this event name
                    .data("new"));       // data content doesn't matter — frontend just refetches
            log.info("[SSE] Pushed trade event to account={}", account);
        } catch (IOException e) {
            // Connection is dead — remove it; browser will reconnect automatically
            clients.remove(account);
            log.warn("[SSE] Failed to push to account={} — removed dead connection", account);
        }
    }

    /**
     * Sends a periodic heartbeat to all connected clients to keep
     * connections alive through proxies/load balancers that close idle connections.
     * Wire this up with @Scheduled in your main app or a scheduler config.
     */
    public void sendHeartbeat() {
        if (clients.isEmpty()) return;
        log.debug("[SSE] Sending heartbeat to {} client(s)", clients.size());

        clients.forEach((account, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                clients.remove(account);
                log.warn("[SSE] Heartbeat failed — removed dead connection for account={}", account);
            }
        });
    }
}