package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationChartVisibility;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonApprovalStatus;
import com.fishdan.myorgchart.person.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrganizationAdminService {

    private final AccountService accountService;
    private final OrganizationAdminRepository organizationAdminRepository;
    private final OrganizationRepository organizationRepository;
    private final PersonRepository personRepository;

    public OrganizationAdminService(
        AccountService accountService,
        OrganizationAdminRepository organizationAdminRepository,
        OrganizationRepository organizationRepository,
        PersonRepository personRepository
    ) {
        this.accountService = accountService;
        this.organizationAdminRepository = organizationAdminRepository;
        this.organizationRepository = organizationRepository;
        this.personRepository = personRepository;
    }

    public List<ManagedOrganizationView> getManagedOrganizations(Long accountId) {
        requireVerifiedAccount(accountId);
        return organizationAdminRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
            .map(OrganizationAdmin::getOrganization)
            .distinct()
            .map(this::buildManagedOrganizationView)
            .toList();
    }

    @Transactional
    public void approveMembership(Long accountId, Long personId) {
        Person person = requireManagedPerson(accountId, personId);
        person.setApprovalStatus(PersonApprovalStatus.APPROVED);
        personRepository.save(person);
    }

    @Transactional
    public void rejectMembership(Long accountId, Long personId) {
        Person person = requireManagedPerson(accountId, personId);
        person.setApprovalStatus(PersonApprovalStatus.REJECTED);
        personRepository.save(person);
    }

    @Transactional
    public void grantAdmin(Long accountId, Long personId) {
        Person person = requireManagedApprovedPerson(accountId, personId);
        Account targetAccount = accountService.getVerifiedAccountByEmail(person.getEmail());
        if (targetAccount == null) {
            throw new IllegalArgumentException("This member needs a verified private account before becoming an admin.");
        }

        if (!organizationAdminRepository.existsByOrganizationIdAndAccountId(person.getOrganization().getId(), targetAccount.getId())) {
            OrganizationAdmin organizationAdmin = new OrganizationAdmin();
            organizationAdmin.setOrganization(person.getOrganization());
            organizationAdmin.setAccount(targetAccount);
            organizationAdmin.setCreatedAt(java.time.Instant.now());
            organizationAdminRepository.save(organizationAdmin);
        }
    }

    @Transactional
    public void revokeAdmin(Long accountId, Long personId) {
        Person person = requireManagedApprovedPerson(accountId, personId);
        Account targetAccount = accountService.getVerifiedAccountByEmail(person.getEmail());
        if (targetAccount == null
            || !organizationAdminRepository.existsByOrganizationIdAndAccountId(person.getOrganization().getId(), targetAccount.getId())) {
            throw new IllegalArgumentException("This member is not currently an admin.");
        }

        long adminCount = organizationAdminRepository.countByOrganizationId(person.getOrganization().getId());
        if (adminCount <= 1) {
            throw new IllegalArgumentException("Official organizations must always have at least one admin.");
        }

        organizationAdminRepository.deleteByOrganizationIdAndAccountId(person.getOrganization().getId(), targetAccount.getId());
    }

    @Transactional
    public void updateChartVisibility(Long accountId, Long organizationId, boolean privateChart) {
        requireVerifiedAccount(accountId);
        if (!organizationAdminRepository.existsByOrganizationIdAndAccountId(organizationId, accountId)) {
            throw new IllegalArgumentException("You can only manage chart privacy for organizations you administer.");
        }

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found."));

        organization.setChartVisibility(privateChart ? OrganizationChartVisibility.PRIVATE : OrganizationChartVisibility.PUBLIC);
        organizationRepository.save(organization);
    }

    private ManagedOrganizationView buildManagedOrganizationView(Organization organization) {
        List<Person> pendingMembers = personRepository.findByOrganizationIdAndApprovalStatusOrderByFullNameAsc(
            organization.getId(),
            PersonApprovalStatus.PROVISIONAL
        );
        List<Person> approvedMembers = personRepository.findByOrganizationIdAndApprovalStatusOrderByFullNameAsc(
            organization.getId(),
            PersonApprovalStatus.APPROVED
        );
        Set<String> adminEmails = organizationAdminRepository.findByOrganizationIdOrderByCreatedAtAsc(organization.getId()).stream()
            .map(admin -> admin.getAccount().getEmail().toLowerCase())
            .collect(Collectors.toSet());
        return new ManagedOrganizationView(organization, pendingMembers, approvedMembers, adminEmails);
    }

    private Person requireManagedApprovedPerson(Long accountId, Long personId) {
        Person person = requireManagedPerson(accountId, personId);
        if (person.getApprovalStatus() != PersonApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved members can hold admin privileges.");
        }
        return person;
    }

    private Person requireManagedPerson(Long accountId, Long personId) {
        requireVerifiedAccount(accountId);
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("Membership not found."));
        if (!organizationAdminRepository.existsByOrganizationIdAndAccountId(person.getOrganization().getId(), accountId)) {
            throw new IllegalArgumentException("You can only manage memberships for organizations you administer.");
        }
        return person;
    }

    private void requireVerifiedAccount(Long accountId) {
        Account account = accountService.getAccountById(accountId);
        if (!account.isVerified()) {
            throw new IllegalArgumentException("Verify your email address before managing official organizations.");
        }
    }
}
