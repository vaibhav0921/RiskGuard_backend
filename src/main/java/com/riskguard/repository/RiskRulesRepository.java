package com.riskguard.repository;

import com.riskguard.entity.RiskRules;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RiskRulesRepository
        extends JpaRepository<RiskRules, Long> {
    Optional<RiskRules> findByEmailAndAccountNumber(String email,
                                                    String accountNumber);
}