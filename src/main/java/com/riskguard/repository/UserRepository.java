package com.riskguard.repository;

import com.riskguard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndAccountNumber(String email,
                                               String accountNumber);
    Optional<User> findByEmail(String email);

    Optional<User> findByAccountNumber(String accountNumber);

}