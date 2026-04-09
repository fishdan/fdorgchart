package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.account.Account;
import com.fishdan.myorgchart.account.AccountService;
import com.fishdan.myorgchart.account.OrganizationAdminRepository;
import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountService accountService;
    private final OrganizationAdminRepository organizationAdminRepository;

    public PersonService(
        PersonRepository personRepository,
        OrganizationRepository organizationRepository,
        AccountService accountService,
        OrganizationAdminRepository organizationAdminRepository
    ) {
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
        this.accountService = accountService;
        this.organizationAdminRepository = organizationAdminRepository;
    }

    public Person createPerson(Person person) {
        return createPerson(person, null);
    }

    public Person createPerson(Person person, String authenticatedEmail) {
        // Normalize domain to lowercase
        String normalizedDomain = person.getDomain().toLowerCase();
        String supervisorEmail = person.getSupervisorEmail();
        String normalizedEmail = person.getEmail().trim().toLowerCase();

        // Ensure the organization domain exists in the database
        Organization organization = organizationRepository.findByDomain(normalizedDomain);
        if (organization == null) {
            throw new IllegalArgumentException("No organization exists with the domain: " + person.getDomain());
        }

        if (organization.isOfficial() && authenticatedEmail == null) {
            throw new IllegalArgumentException(
                "This organization is officially managed. Sign in with a verified private account to request membership."
            );
        }

        Account verifiedAccount = accountService.getVerifiedAccountByEmail(normalizedEmail);
        if (verifiedAccount != null
            && (authenticatedEmail == null || !verifiedAccount.getEmail().equalsIgnoreCase(authenticatedEmail))) {
            throw new IllegalArgumentException(
                normalizedEmail
                    + " has a private account. Please send an email to "
                    + normalizedEmail
                    + " asking them to add themselves to "
                    + organization.getName()
            );
        }

        person.setOrganization(organization);
        person.setDomain(normalizedDomain); // Store in lowercase for consistency
        person.setEmail(normalizedEmail);
        if (supervisorEmail != null && supervisorEmail.isBlank()) {
            person.setSupervisorEmail(null);
        } else if (supervisorEmail != null) {
            person.setSupervisorEmail(supervisorEmail.trim().toLowerCase());
        }

        if (organization.isOfficial()) {
            boolean isExistingAdmin = authenticatedEmail != null
                && organizationAdminRepository.existsByOrganizationDomainIgnoreCaseAndAccountEmail(
                    normalizedDomain,
                    authenticatedEmail.trim().toLowerCase()
                );
            person.setApprovalStatus(isExistingAdmin ? PersonApprovalStatus.APPROVED : PersonApprovalStatus.PROVISIONAL);
        } else {
            person.setApprovalStatus(PersonApprovalStatus.APPROVED);
        }

        // Check for uniqueness of email + domain
        if (personRepository.existsByEmailAndDomain(normalizedEmail, normalizedDomain)) {
            throw new IllegalArgumentException("A person with this email and domain already exists.");
        }

        return personRepository.save(person);
    }

}
