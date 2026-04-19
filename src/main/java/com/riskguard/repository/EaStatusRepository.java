// src/main/java/com/riskguard/repository/EaStatusRepository.java
package com.riskguard.repository;

import com.riskguard.entity.EaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EaStatusRepository
        extends JpaRepository<EaStatus, Long> {
    Optional<EaStatus> findByEmailAndAccountNumber(
            String email, String accountNumber);
}