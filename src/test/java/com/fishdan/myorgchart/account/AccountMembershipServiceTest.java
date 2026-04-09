package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonApprovalStatus;
import com.fishdan.myorgchart.person.PersonRepository;
import com.fishdan.myorgchart.person.PersonService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AccountMembershipServiceTest {

    @Test
    void addSelfToOrganizationRequiresVerifiedAccount() {
        AccountService accountService = mock(AccountService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonService personService = mock(PersonService.class);
        AccountMembershipService membershipService =
            new AccountMembershipService(accountService, personRepository, personService);

        Account account = new Account();
        account.setId(4L);
        account.setEmail("person@example.com");

        when(accountService.getAccountById(4L)).thenReturn(account);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> membershipService.addSelfToOrganization(
                4L,
                "Person Example",
                "fishdan.com",
                "Engineering",
                null
            )
        );

        assertEquals("Verify your email address before managing organization memberships.", exception.getMessage());
    }

    @Test
    void updateOwnMembershipRejectsEditingAnotherUsersEntry() {
        AccountService accountService = mock(AccountService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonService personService = mock(PersonService.class);
        AccountMembershipService membershipService =
            new AccountMembershipService(accountService, personRepository, personService);

        Account account = new Account();
        account.setId(4L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        Person person = new Person();
        person.setId(12L);
        person.setEmail("other@example.com");

        when(accountService.getAccountById(4L)).thenReturn(account);
        when(personRepository.findById(12L)).thenReturn(Optional.of(person));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> membershipService.updateOwnMembership(4L, 12L, "Engineering", null)
        );

        assertEquals("You can only edit your own organization memberships.", exception.getMessage());
    }

    @Test
    void addSelfToOrganizationUsesVerifiedAccountsEmail() {
        AccountService accountService = mock(AccountService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonService personService = mock(PersonService.class);
        AccountMembershipService membershipService =
            new AccountMembershipService(accountService, personRepository, personService);

        Account account = new Account();
        account.setId(4L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        when(accountService.getAccountById(4L)).thenReturn(account);

        membershipService.addSelfToOrganization(4L, "Owner", "fishdan.com", "Engineering", null);

        verify(personService).createPerson(any(Person.class), eq("owner@example.com"));
    }

    @Test
    void updateOwnMembershipRejectsRejectedMemberships() {
        AccountService accountService = mock(AccountService.class);
        PersonRepository personRepository = mock(PersonRepository.class);
        PersonService personService = mock(PersonService.class);
        AccountMembershipService membershipService =
            new AccountMembershipService(accountService, personRepository, personService);

        Account account = new Account();
        account.setId(4L);
        account.setEmail("owner@example.com");
        account.setEmailVerifiedAt(Instant.parse("2026-04-09T12:00:00Z"));

        Person person = new Person();
        person.setId(12L);
        person.setEmail("owner@example.com");
        person.setApprovalStatus(PersonApprovalStatus.REJECTED);

        when(accountService.getAccountById(4L)).thenReturn(account);
        when(personRepository.findById(12L)).thenReturn(Optional.of(person));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> membershipService.updateOwnMembership(4L, 12L, "Engineering", null)
        );

        assertEquals("Rejected memberships cannot be edited.", exception.getMessage());
    }
}
