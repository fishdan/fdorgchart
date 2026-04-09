package com.fishdan.myorgchart.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);
    long countByEmailAndCreatedAtAfter(String email, Instant createdAt);
    List<EmailVerificationCode> findByAccountIdAndStatusOrderByCreatedAtDesc(
        Long accountId,
        EmailVerificationCodeStatus status
    );
    Optional<EmailVerificationCode> findFirstByAccountIdAndStatusOrderByCreatedAtDesc(
        Long accountId,
        EmailVerificationCodeStatus status
    );
    long deleteByEmail(String email);
}
