// src/main/java/com/riskguard/repository/TradeRepository.java
package com.riskguard.repository;

import com.riskguard.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // All trades for an account in date range, newest first
    List<Trade> findByAccountOrderByCloseTimeDesc(String account);

    List<Trade> findByAccountAndTradeDateBetweenOrderByCloseTimeDesc(
            String account, LocalDate from, LocalDate to);

    // Check for duplicate ticket so EA retries don't double-insert
    boolean existsByAccountAndTicket(String account, Long ticket);

    // For streak calculation — distinct dates that had any breach
    @Query("SELECT DISTINCT t.tradeDate FROM Trade t " +
            "WHERE t.account = :account AND t.disciplineBreached = true " +
            "ORDER BY t.tradeDate DESC")
    List<LocalDate> findBreachedDates(@Param("account") String account);

    // Distinct dates that had trades (for total-days-tracked count)
    @Query("SELECT DISTINCT t.tradeDate FROM Trade t " +
            "WHERE t.account = :account ORDER BY t.tradeDate DESC")
    List<LocalDate> findAllTradeDates(@Param("account") String account);
}