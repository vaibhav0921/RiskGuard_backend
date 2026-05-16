// src/main/java/com/riskguard/service/TradeService.java
package com.riskguard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.entity.Trade;
import com.riskguard.repository.TradeRepository;
import com.riskguard.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final SseService      sseService;

    private static final DateTimeFormatter MT5_FMT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Called by TradeController POST /api/trades/close ─────────────────────

    public Map<String, Object> recordTrade(Map<String, Object> body) {
        String account = (String) body.get("account");
        Long   ticket  = toLong(body.get("ticket"));

        // Idempotency — EA may retry on network error
        if (tradeRepository.existsByAccountAndTicket(account, ticket)) {
            log.info("[Trade] Duplicate ticket {} for account {} — skipped", ticket, account);
            return Map.of("status", "duplicate");
        }

        // rulesBroken arrives either as:
        //   - a Java List  (Jackson parsed the JSON array  e.g. ["CONSECUTIVE_LOSS"])
        //   - a String     (already serialised            e.g. "[\"CONSECUTIVE_LOSS\"]")
        //   - null         (field absent)
        String rulesBroken = toJsonArrayString(body.get("rulesBroken"));

        LocalDateTime closeTime = parseDateTime(body.get("closeTime"));
        LocalDateTime openTime  = parseDateTime(body.get("openTime"));
        LocalDate     tradeDate = closeTime != null ? closeTime.toLocalDate() : LocalDate.now();

        boolean breached = !rulesBroken.equals("[]");

        Trade trade = Trade.builder()
                .account(account)
                .email((String) body.get("email"))
                .ticket(ticket)
                .symbol((String) body.get("symbol"))
                .type((String) body.get("type"))
                .volume(toDouble(body.get("volume")))
                .openPrice(toDouble(body.get("openPrice")))
                .closePrice(toDouble(body.get("closePrice")))
                .profit(toDouble(body.get("profit")))
                .closeReason((String) body.get("closeReason"))
                .rulesBroken(rulesBroken)
                .openTime(openTime)
                .closeTime(closeTime)
                .tradeDate(tradeDate)
                .disciplineBreached(breached)
                .build();

        tradeRepository.save(trade);
        log.info("[Trade] Saved — account={} ticket={} symbol={} profit={} reason={} breached={}",
                account, ticket, trade.getSymbol(), trade.getProfit(),
                trade.getCloseReason(), breached);

        // Push real-time event to the frontend — triggers instant UI update
        sseService.pushTradeEvent(account);

        return Map.of("status", "saved");
    }

    // ── Called by TradeController GET /api/trades/history ────────────────────

    public Map<String, Object> getHistory(String account, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        LocalDate to   = LocalDate.now();

        List<Trade> trades = tradeRepository
                .findByAccountAndTradeDateBetweenOrderByCloseTimeDesc(account, from, to);

        log.info("[TradeHistory] account={} found {} trades over {} days",
                account, trades.size(), days);

        // Group by date — LinkedHashMap preserves DESC insertion order from query
        Map<LocalDate, List<Trade>> byDate = trades.stream()
                .collect(Collectors.groupingBy(Trade::getTradeDate,
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> daysList = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Trade>> entry : byDate.entrySet()) {
            LocalDate   date      = entry.getKey();
            List<Trade> dayTrades = entry.getValue();
            boolean     breached  = dayTrades.stream().anyMatch(Trade::getDisciplineBreached);
            double      dayProfit = dayTrades.stream()
                    .mapToDouble(t -> t.getProfit() != null ? t.getProfit() : 0)
                    .sum();

            List<Map<String, Object>> tradeList = dayTrades.stream()
                    .map(this::tradeToMap)
                    .collect(Collectors.toList());

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date",        date.toString());
            day.put("disciplined", !breached);
            day.put("totalProfit", Math.round(dayProfit * 100.0) / 100.0);
            day.put("tradesCount", dayTrades.size());
            day.put("trades",      tradeList);
            daysList.add(day);
        }

        // Discipline stats
        List<LocalDate> allDates      = tradeRepository.findAllTradeDates(account);
        List<LocalDate> breachedDates = tradeRepository.findBreachedDates(account);
        Set<LocalDate>  breachedSet   = new HashSet<>(breachedDates);

        int totalDays       = allDates.size();
        int disciplinedDays = (int) allDates.stream().filter(d -> !breachedSet.contains(d)).count();
        int currentStreak   = calcCurrentStreak(allDates, breachedSet);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days",            daysList);
        result.put("totalDays",       totalDays);
        result.put("disciplinedDays", disciplinedDays);
        result.put("currentStreak",   currentStreak);
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Safely converts whatever Jackson gives us for a JSON array field into
     * a properly serialised JSON array string for the TEXT column.
     *
     * Cases:
     *   null                          → "[]"
     *   List  (["A","B"])             → "[\"A\",\"B\"]"   via ObjectMapper
     *   String "[]"                   → "[]"
     *   String "[\"A\"]"              → "[\"A\"]"
     */
    private String toJsonArrayString(Object val) {
        if (val == null) return "[]";

        if (val instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) val;
            if (list.isEmpty()) return "[]";
            try {
                return MAPPER.writeValueAsString(list);
            } catch (Exception e) {
                log.warn("[Trade] Failed to serialise rulesBroken list: {}", e.getMessage());
                return "[]";
            }
        }

        // Already a String — validate it looks like a JSON array
        String s = val.toString().trim();
        if (s.startsWith("[") && s.endsWith("]")) return s;

        // Unexpected format — log and default
        log.warn("[Trade] Unexpected rulesBroken format: {}", s);
        return "[]";
    }

    private int calcCurrentStreak(List<LocalDate> allDates, Set<LocalDate> breachedSet) {
        // allDates is ordered DESC from query
        int streak = 0;
        for (LocalDate d : allDates) {
            if (breachedSet.contains(d)) break;
            streak++;
        }
        return streak;
    }

    private Map<String, Object> tradeToMap(Trade t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ticket",             t.getTicket());
        m.put("symbol",             t.getSymbol());
        m.put("type",               t.getType());
        m.put("volume",             t.getVolume());
        m.put("openPrice",          t.getOpenPrice());
        m.put("closePrice",         t.getClosePrice());
        m.put("profit",             t.getProfit());
        m.put("closeReason",        t.getCloseReason());
        m.put("rulesBroken",        t.getRulesBroken());
        m.put("openTime",           t.getOpenTime()  != null ? t.getOpenTime().toString()  : null);
        m.put("closeTime",          t.getCloseTime() != null ? t.getCloseTime().toString() : null);
        m.put("disciplineBreached", t.getDisciplineBreached());
        return m;
    }

    private LocalDateTime parseDateTime(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        try {
            // MT5 format: "2024.01.15 14:22:00"
            if (s.contains(".")) return LocalDateTime.parse(s, MT5_FMT);
            // ISO format fallback
            return LocalDateTime.parse(s.replace(" ", "T"));
        } catch (Exception e) {
            log.warn("[Trade] Could not parse datetime '{}': {}", s, e.getMessage());
            return null;
        }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }
}