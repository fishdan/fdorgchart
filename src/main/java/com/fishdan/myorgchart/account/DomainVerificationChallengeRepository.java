package com.fishdan.myorgchart.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomainVerificationChallengeRepository extends JpaRepository<DomainVerificationChallenge, Long> {

    List<DomainVerificationChallenge> findByRequestedByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<DomainVerificationChallenge> findByDomainAndStatusOrderByCreatedAtDesc(
        String domain,
        DomainVerificationChallengeStatus status
    );

    long deleteByRequestedByAccountEmail(String email);

    long deleteByDomainIgnoreCase(String domain);
}
