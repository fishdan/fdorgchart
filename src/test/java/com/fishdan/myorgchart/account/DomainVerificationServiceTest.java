package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationOwnership;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainVerificationServiceTest {

    @Test
    void startChallengeRejectsAlreadyOfficialOrganization() {
        AccountService accountService = mock(AccountService.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        DomainVerificationChallengeRepository challengeRepository = mock(DomainVerificationChallengeRepository.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        DnsTxtLookup dnsTxtLookup = mock(DnsTxtLookup.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        DomainVerificationService service = new DomainVerificationService(
            accountService,
            organizationRepository,
            challengeRepository,
            organizationAdminRepository,
            dnsTxtLookup,
            passwordEncoder,
            clock
        );

        Account account = new Account();
        account.setId(7L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T11:00:00Z"));

        Organization organization = new Organization();
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);

        when(accountService.getAccountById(7L)).thenReturn(account);
        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.startChallenge(7L, "Fishdan.com")
        );

        assertEquals("This organization is already official and cannot be claimed again.", exception.getMessage());
    }

    @Test
    void verifyChallengeRejectsChecksInsideTenMinuteWindow() {
        AccountService accountService = mock(AccountService.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        DomainVerificationChallengeRepository challengeRepository = mock(DomainVerificationChallengeRepository.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        DnsTxtLookup dnsTxtLookup = mock(DnsTxtLookup.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        DomainVerificationService service = new DomainVerificationService(
            accountService,
            organizationRepository,
            challengeRepository,
            organizationAdminRepository,
            dnsTxtLookup,
            passwordEncoder,
            clock
        );

        Account account = new Account();
        account.setId(7L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T11:00:00Z"));

        DomainVerificationChallenge challenge = new DomainVerificationChallenge();
        challenge.setId(11L);
        challenge.setRequestedByAccount(account);
        challenge.setDomain("fishdan.com");
        challenge.setChallengeToken("fdorgchart-verification=abc");
        challenge.setStatus(DomainVerificationChallengeStatus.FAILED);
        challenge.setLastCheckedAt(Instant.parse("2026-04-09T11:55:00Z"));

        when(accountService.getAccountById(7L)).thenReturn(account);
        when(challengeRepository.findById(11L)).thenReturn(Optional.of(challenge));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.verifyChallenge(7L, 11L)
        );

        assertEquals("Please wait 10 minutes before checking this domain again.", exception.getMessage());
    }

    @Test
    void verifyChallengeClaimsExistingOpenOrganizationAndCreatesFirstAdmin() {
        AccountService accountService = mock(AccountService.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        DomainVerificationChallengeRepository challengeRepository = mock(DomainVerificationChallengeRepository.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        DnsTxtLookup dnsTxtLookup = mock(DnsTxtLookup.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        DomainVerificationService service = new DomainVerificationService(
            accountService,
            organizationRepository,
            challengeRepository,
            organizationAdminRepository,
            dnsTxtLookup,
            passwordEncoder,
            clock
        );

        Account account = new Account();
        account.setId(7L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T11:00:00Z"));

        Organization organization = new Organization();
        organization.setId(21L);
        organization.setName("Fishdan");
        organization.setDomain("fishdan.com");
        organization.setOwnershipType(OrganizationOwnership.OPEN);

        DomainVerificationChallenge challenge = new DomainVerificationChallenge();
        challenge.setId(11L);
        challenge.setRequestedByAccount(account);
        challenge.setOrganization(organization);
        challenge.setDomain("fishdan.com");
        challenge.setChallengeToken("fdorgchart-verification=abc");
        challenge.setStatus(DomainVerificationChallengeStatus.PENDING);

        when(accountService.getAccountById(7L)).thenReturn(account);
        when(challengeRepository.findById(11L)).thenReturn(Optional.of(challenge));
        when(dnsTxtLookup.lookupTxtRecords("fishdan.com")).thenReturn(List.of("fdorgchart-verification=abc"));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationAdminRepository.existsByOrganizationIdAndAccountId(21L, 7L)).thenReturn(false);

        Organization result = service.verifyChallenge(7L, 11L);

        assertSame(organization, result);
        assertEquals(OrganizationOwnership.OFFICIAL, organization.getOwnershipType());
        assertEquals(DomainVerificationChallengeStatus.VERIFIED, challenge.getStatus());
        verify(organizationAdminRepository).save(any(OrganizationAdmin.class));
        verify(challengeRepository).save(challenge);
    }

    @Test
    void verifyChallengeCreatesNewOfficialOrganizationWhenNoneExists() {
        AccountService accountService = mock(AccountService.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        DomainVerificationChallengeRepository challengeRepository = mock(DomainVerificationChallengeRepository.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        DnsTxtLookup dnsTxtLookup = mock(DnsTxtLookup.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        DomainVerificationService service = new DomainVerificationService(
            accountService,
            organizationRepository,
            challengeRepository,
            organizationAdminRepository,
            dnsTxtLookup,
            passwordEncoder,
            clock
        );

        Account account = new Account();
        account.setId(7L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T11:00:00Z"));

        DomainVerificationChallenge challenge = new DomainVerificationChallenge();
        challenge.setId(11L);
        challenge.setRequestedByAccount(account);
        challenge.setDomain("newdomain.com");
        challenge.setChallengeToken("fdorgchart-verification=abc");
        challenge.setStatus(DomainVerificationChallengeStatus.PENDING);

        when(accountService.getAccountById(7L)).thenReturn(account);
        when(challengeRepository.findById(11L)).thenReturn(Optional.of(challenge));
        when(dnsTxtLookup.lookupTxtRecords("newdomain.com")).thenReturn(List.of("fdorgchart-verification=abc"));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-password");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization organization = invocation.getArgument(0);
            organization.setId(33L);
            return organization;
        });
        when(organizationAdminRepository.existsByOrganizationIdAndAccountId(33L, 7L)).thenReturn(false);

        Organization result = service.verifyChallenge(7L, 11L);

        assertEquals("newdomain.com", result.getDomain());
        assertEquals("newdomain.com", result.getName());
        assertEquals("owner@example.com", result.getEmail());
        assertEquals(OrganizationOwnership.OFFICIAL, result.getOwnershipType());
        verify(organizationAdminRepository).save(any(OrganizationAdmin.class));
        verify(challengeRepository).save(challenge);
    }

    @Test
    void verifyChallengeMarksFailureWhenTxtRecordIsMissing() {
        AccountService accountService = mock(AccountService.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        DomainVerificationChallengeRepository challengeRepository = mock(DomainVerificationChallengeRepository.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        DnsTxtLookup dnsTxtLookup = mock(DnsTxtLookup.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        DomainVerificationService service = new DomainVerificationService(
            accountService,
            organizationRepository,
            challengeRepository,
            organizationAdminRepository,
            dnsTxtLookup,
            passwordEncoder,
            clock
        );

        Account account = new Account();
        account.setId(7L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T11:00:00Z"));

        DomainVerificationChallenge challenge = new DomainVerificationChallenge();
        challenge.setId(11L);
        challenge.setRequestedByAccount(account);
        challenge.setDomain("fishdan.com");
        challenge.setChallengeToken("fdorgchart-verification=abc");
        challenge.setStatus(DomainVerificationChallengeStatus.PENDING);

        when(accountService.getAccountById(7L)).thenReturn(account);
        when(challengeRepository.findById(11L)).thenReturn(Optional.of(challenge));
        when(dnsTxtLookup.lookupTxtRecords("fishdan.com")).thenReturn(List.of("some-other-record"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.verifyChallenge(7L, 11L)
        );

        assertEquals("TXT verification record not found yet. Publish the record and try again later.", exception.getMessage());
        assertEquals(DomainVerificationChallengeStatus.FAILED, challenge.getStatus());
        verify(challengeRepository).save(challenge);
    }
}
