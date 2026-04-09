package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.account.Account;
import com.fishdan.myorgchart.account.AccountService;
import com.fishdan.myorgchart.account.OrganizationAdminRepository;
import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationOwnership;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonServiceTest {

    @Test
    void createPersonNormalizesBlankSupervisorEmailToNull() {
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        PersonService personService = new PersonService(
            personRepository,
            organizationRepository,
            accountService,
            organizationAdminRepository
        );

        Organization organization = new Organization();
        Person person = new Person();
        person.setFullName("Daniel Fishman");
        person.setEmail("dan@fishdan.com");
        person.setDomain("fishdan.com");
        person.setDepartment("CEO");
        person.setSupervisorEmail("");

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);
        when(personRepository.existsByEmailAndDomain("dan@fishdan.com", "fishdan.com")).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personService.createPerson(person);

        ArgumentCaptor<Person> savedPerson = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(savedPerson.capture());
        assertNull(savedPerson.getValue().getSupervisorEmail());
    }

    @Test
    void createPersonRejectsReservedVerifiedEmailForNonOwner() {
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        PersonService personService = new PersonService(
            personRepository,
            organizationRepository,
            accountService,
            organizationAdminRepository
        );

        Organization organization = new Organization();
        organization.setName("Fishdan");

        Account account = new Account();
        account.setEmail("dan@fishdan.com");
        account.setEmailVerifiedAt(java.time.Instant.parse("2026-04-09T12:00:00Z"));

        Person person = new Person();
        person.setFullName("Daniel Fishman");
        person.setEmail("dan@fishdan.com");
        person.setDomain("fishdan.com");
        person.setDepartment("CEO");

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);
        when(accountService.getVerifiedAccountByEmail("dan@fishdan.com")).thenReturn(account);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personService.createPerson(person, null)
        );

        assertEquals(
            "dan@fishdan.com has a private account. Please send an email to dan@fishdan.com asking them to add themselves to Fishdan",
            exception.getMessage()
        );
    }

    @Test
    void createPersonRejectsPublicAddsToOfficialOrganizations() {
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        AccountService accountService = mock(AccountService.class);
        OrganizationAdminRepository organizationAdminRepository = mock(OrganizationAdminRepository.class);
        PersonService personService = new PersonService(
            personRepository,
            organizationRepository,
            accountService,
            organizationAdminRepository
        );

        Organization organization = new Organization();
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);

        Person person = new Person();
        person.setFullName("Daniel Fishman");
        person.setEmail("dan@fishdan.com");
        person.setDomain("fishdan.com");
        person.setDepartment("CEO");

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> personService.createPerson(person, null)
        );

        assertEquals(
            "This organization is officially managed. Sign in with a verified private account to request membership.",
            exception.getMessage()
        );
    }
}
