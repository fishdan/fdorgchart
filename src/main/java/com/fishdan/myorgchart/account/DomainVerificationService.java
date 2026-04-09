package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationOwnership;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DomainVerificationService {

    static final Duration VERIFICATION_CHECK_WINDOW = Duration.ofMinutes(10);

    private final AccountService accountService;
    private final OrganizationRepository organizationRepository;
    private final DomainVerificationChallengeRepository challengeRepository;
    private final OrganizationAdminRepository organizationAdminRepository;
    private final DnsTxtLookup dnsTxtLookup;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DomainVerificationService(
        AccountService accountService,
        OrganizationRepository organizationRepository,
        DomainVerificationChallengeRepository challengeRepository,
        OrganizationAdminRepository organizationAdminRepository,
        DnsTxtLookup dnsTxtLookup,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.accountService = accountService;
        this.organizationRepository = organizationRepository;
        this.challengeRepository = challengeRepository;
        this.organizationAdminRepository = organizationAdminRepository;
        this.dnsTxtLookup = dnsTxtLookup;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public List<DomainVerificationChallenge> getChallengesForAccount(Long accountId) {
        return challengeRepository.findByRequestedByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public List<OrganizationAdmin> getAdminOrganizations(Long accountId) {
        return organizationAdminRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional
    public DomainVerificationChallenge startChallenge(Long accountId, String domain) {
        Account account = requireVerifiedAccount(accountId);
        String normalizedDomain = normalizeDomain(domain);
        Organization existingOrganization = organizationRepository.findByDomain(normalizedDomain);
        if (existingOrganization != null && existingOrganization.isOfficial()) {
            throw new IllegalArgumentException("This organization is already official and cannot be claimed again.");
        }

        List<DomainVerificationChallenge> pendingChallenges =
            challengeRepository.findByDomainAndStatusOrderByCreatedAtDesc(
                normalizedDomain,
                DomainVerificationChallengeStatus.PENDING
            );
        if (!pendingChallenges.isEmpty()) {
            for (DomainVerificationChallenge pendingChallenge : pendingChallenges) {
                pendingChallenge.setStatus(DomainVerificationChallengeStatus.SUPERSEDED);
            }
            challengeRepository.saveAll(pendingChallenges);
        }

        DomainVerificationChallenge challenge = new DomainVerificationChallenge();
        challenge.setRequestedByAccount(account);
        challenge.setOrganization(existingOrganization);
        challenge.setDomain(normalizedDomain);
        challenge.setChallengeToken("fdorgchart-verification=" + UUID.randomUUID());
        challenge.setStatus(DomainVerificationChallengeStatus.PENDING);
        challenge.setCreatedAt(clock.instant());
        return challengeRepository.save(challenge);
    }

    @Transactional
    public Organization verifyChallenge(Long accountId, Long challengeId) {
        Account account = requireVerifiedAccount(accountId);
        DomainVerificationChallenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Domain verification challenge not found."));

        if (!challenge.getRequestedByAccount().getId().equals(account.getId())) {
            throw new IllegalArgumentException("You can only verify your own domain challenges.");
        }

        if (challenge.getStatus() == DomainVerificationChallengeStatus.VERIFIED) {
            throw new IllegalArgumentException("This domain challenge has already been verified.");
        }
        if (challenge.getStatus() == DomainVerificationChallengeStatus.SUPERSEDED) {
            throw new IllegalArgumentException("This domain challenge was replaced. Start a new one.");
        }

        Instant now = clock.instant();
        if (challenge.getLastCheckedAt() != null
            && challenge.getLastCheckedAt().plus(VERIFICATION_CHECK_WINDOW).isAfter(now)) {
            throw new IllegalArgumentException("Please wait 10 minutes before checking this domain again.");
        }

        challenge.setLastCheckedAt(now);
        List<String> txtRecords = dnsTxtLookup.lookupTxtRecords(challenge.getDomain());
        boolean found = txtRecords.stream().anyMatch(record -> record.contains(challenge.getChallengeToken()));
        if (!found) {
            challenge.setStatus(DomainVerificationChallengeStatus.FAILED);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException("TXT verification record not found yet. Publish the record and try again later.");
        }

        Organization organization = challenge.getOrganization();
        if (organization == null) {
            organization = new Organization();
            organization.setName(challenge.getDomain());
            organization.setDomain(challenge.getDomain());
            organization.setEmail(account.getEmail());
            organization.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }

        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);
        Organization savedOrganization = organizationRepository.save(organization);

        if (!organizationAdminRepository.existsByOrganizationIdAndAccountId(savedOrganization.getId(), account.getId())) {
            OrganizationAdmin organizationAdmin = new OrganizationAdmin();
            organizationAdmin.setOrganization(savedOrganization);
            organizationAdmin.setAccount(account);
            organizationAdmin.setCreatedAt(now);
            organizationAdminRepository.save(organizationAdmin);
        }

        challenge.setOrganization(savedOrganization);
        challenge.setStatus(DomainVerificationChallengeStatus.VERIFIED);
        challenge.setVerifiedAt(now);
        challengeRepository.save(challenge);
        return savedOrganization;
    }

    private Account requireVerifiedAccount(Long accountId) {
        Account account = accountService.getAccountById(accountId);
        if (!account.isVerified()) {
            throw new IllegalArgumentException("Verify your email address before managing official domains.");
        }
        return account;
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new IllegalArgumentException("Domain is required.");
        }
        return domain.trim().toLowerCase();
    }
}
