package com.fishdan.myorgchart.account;

import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonApprovalStatus;
import com.fishdan.myorgchart.person.PersonRepository;
import com.fishdan.myorgchart.person.PersonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountMembershipService {

    private final AccountService accountService;
    private final PersonRepository personRepository;
    private final PersonService personService;

    public AccountMembershipService(
        AccountService accountService,
        PersonRepository personRepository,
        PersonService personService
    ) {
        this.accountService = accountService;
        this.personRepository = personRepository;
        this.personService = personService;
    }

    public List<Person> getMemberships(Long accountId) {
        Account account = accountService.getAccountById(accountId);
        return personRepository.findByEmailIgnoreCase(account.getEmail());
    }

    @Transactional
    public void addSelfToOrganization(
        Long accountId,
        String fullName,
        String domain,
        String department,
        String supervisorEmail
    ) {
        Account account = requireVerifiedAccount(accountId);

        Person person = new Person();
        person.setFullName(fullName);
        person.setEmail(account.getEmail());
        person.setDomain(domain);
        person.setDepartment(department);
        person.setSupervisorEmail(supervisorEmail);

        personService.createPerson(person, account.getEmail());
    }

    @Transactional
    public void updateOwnMembership(
        Long accountId,
        Long personId,
        String department,
        String supervisorEmail
    ) {
        Account account = requireVerifiedAccount(accountId);
        Person person = personRepository.findById(personId)
            .orElseThrow(() -> new IllegalArgumentException("Membership not found."));

        if (!person.getEmail().equalsIgnoreCase(account.getEmail())) {
            throw new IllegalArgumentException("You can only edit your own organization memberships.");
        }

        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("Department is required.");
        }

        if (person.getApprovalStatus() == PersonApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Rejected memberships cannot be edited.");
        }

        person.setDepartment(department.trim());
        person.setSupervisorEmail(
            supervisorEmail == null || supervisorEmail.isBlank() ? null : supervisorEmail.trim().toLowerCase()
        );
        personRepository.save(person);
    }

    private Account requireVerifiedAccount(Long accountId) {
        Account account = accountService.getAccountById(accountId);
        if (!account.isVerified()) {
            throw new IllegalArgumentException("Verify your email address before managing organization memberships.");
        }
        return account;
    }
}
