package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationOwnership;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonApprovalStatus;
import com.fishdan.myorgchart.person.PersonRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrganizationAdminServiceTest {

    @Test
    void approveMembershipRequiresAdminAccessToOrganization() {
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository adminRepository = mock(OrganizationAdminRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationAdminService service =
            new OrganizationAdminService(accountService, adminRepository, organizationRepository, personRepository);

        Account actingAccount = new Account();
        actingAccount.setId(1L);
        actingAccount.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        Organization organization = new Organization();
        organization.setId(3L);
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);

        Person person = new Person();
        person.setId(9L);
        person.setOrganization(organization);
        person.setApprovalStatus(PersonApprovalStatus.PROVISIONAL);

        when(accountService.getAccountById(1L)).thenReturn(actingAccount);
        when(personRepository.findById(9L)).thenReturn(Optional.of(person));
        when(adminRepository.existsByOrganizationIdAndAccountId(3L, 1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.approveMembership(1L, 9L)
        );

        assertEquals("You can only manage memberships for organizations you administer.", exception.getMessage());
    }

    @Test
    void grantAdminRequiresVerifiedPrivateAccountForMember() {
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository adminRepository = mock(OrganizationAdminRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationAdminService service =
            new OrganizationAdminService(accountService, adminRepository, organizationRepository, personRepository);

        Account actingAccount = new Account();
        actingAccount.setId(1L);
        actingAccount.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        Organization organization = new Organization();
        organization.setId(3L);
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);

        Person person = new Person();
        person.setId(9L);
        person.setEmail("member@fishdan.com");
        person.setOrganization(organization);
        person.setApprovalStatus(PersonApprovalStatus.APPROVED);

        when(accountService.getAccountById(1L)).thenReturn(actingAccount);
        when(personRepository.findById(9L)).thenReturn(Optional.of(person));
        when(adminRepository.existsByOrganizationIdAndAccountId(3L, 1L)).thenReturn(true);
        when(accountService.getVerifiedAccountByEmail("member@fishdan.com")).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.grantAdmin(1L, 9L)
        );

        assertEquals("This member needs a verified private account before becoming an admin.", exception.getMessage());
    }

    @Test
    void revokeAdminProtectsLastAdmin() {
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository adminRepository = mock(OrganizationAdminRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationAdminService service =
            new OrganizationAdminService(accountService, adminRepository, organizationRepository, personRepository);

        Account actingAccount = new Account();
        actingAccount.setId(1L);
        actingAccount.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        Account targetAccount = new Account();
        targetAccount.setId(2L);
        targetAccount.setEmail("admin@fishdan.com");
        targetAccount.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        Organization organization = new Organization();
        organization.setId(3L);
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);

        Person person = new Person();
        person.setId(9L);
        person.setEmail("admin@fishdan.com");
        person.setOrganization(organization);
        person.setApprovalStatus(PersonApprovalStatus.APPROVED);

        when(accountService.getAccountById(1L)).thenReturn(actingAccount);
        when(personRepository.findById(9L)).thenReturn(Optional.of(person));
        when(adminRepository.existsByOrganizationIdAndAccountId(3L, 1L)).thenReturn(true);
        when(accountService.getVerifiedAccountByEmail("admin@fishdan.com")).thenReturn(targetAccount);
        when(adminRepository.existsByOrganizationIdAndAccountId(3L, 2L)).thenReturn(true);
        when(adminRepository.countByOrganizationId(3L)).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.revokeAdmin(1L, 9L)
        );

        assertEquals("Official organizations must always have at least one admin.", exception.getMessage());
    }

    @Test
    void updateChartVisibilityRequiresAdminAccess() {
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository adminRepository = mock(OrganizationAdminRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationAdminService service =
            new OrganizationAdminService(accountService, adminRepository, organizationRepository, personRepository);

        Account actingAccount = new Account();
        actingAccount.setId(1L);
        actingAccount.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        when(accountService.getAccountById(1L)).thenReturn(actingAccount);
        when(adminRepository.existsByOrganizationIdAndAccountId(3L, 1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.updateChartVisibility(1L, 3L, true)
        );

        assertEquals("You can only manage chart privacy for organizations you administer.", exception.getMessage());
    }
}
