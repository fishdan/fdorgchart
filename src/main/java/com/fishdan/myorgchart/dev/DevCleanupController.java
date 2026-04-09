package com.fishdan.myorgchart.dev;

import com.fishdan.myorgchart.account.AccountRepository;
import com.fishdan.myorgchart.account.DevDnsTxtStore;
import com.fishdan.myorgchart.account.DevVerificationInbox;
import com.fishdan.myorgchart.account.DomainVerificationChallengeRepository;
import com.fishdan.myorgchart.account.EmailVerificationCodeRepository;
import com.fishdan.myorgchart.account.OrganizationAdminRepository;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import com.fishdan.myorgchart.person.PersonRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/test-data")
public class DevCleanupController {

    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountRepository accountRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final DomainVerificationChallengeRepository domainVerificationChallengeRepository;
    private final OrganizationAdminRepository organizationAdminRepository;
    private final DevVerificationInbox devVerificationInbox;
    private final DevDnsTxtStore devDnsTxtStore;

    public DevCleanupController(
        PersonRepository personRepository,
        OrganizationRepository organizationRepository,
        AccountRepository accountRepository,
        EmailVerificationCodeRepository emailVerificationCodeRepository,
        DomainVerificationChallengeRepository domainVerificationChallengeRepository,
        OrganizationAdminRepository organizationAdminRepository,
        DevVerificationInbox devVerificationInbox,
        DevDnsTxtStore devDnsTxtStore
    ) {
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
        this.accountRepository = accountRepository;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.domainVerificationChallengeRepository = domainVerificationChallengeRepository;
        this.organizationAdminRepository = organizationAdminRepository;
        this.devVerificationInbox = devVerificationInbox;
        this.devDnsTxtStore = devDnsTxtStore;
    }

    @DeleteMapping("/organization")
    @Transactional
    public Map<String, Long> deleteOrganizationHierarchy(@RequestParam String domain) {
        long deletedAdmins = organizationAdminRepository.deleteByOrganizationDomainIgnoreCase(domain);
        long deletedChallenges = domainVerificationChallengeRepository.deleteByDomainIgnoreCase(domain);
        long deletedPeople = personRepository.deleteByDomainIgnoreCase(domain);
        long deletedOrganizations = organizationRepository.deleteByDomainIgnoreCase(domain);
        devDnsTxtStore.remove(domain.trim().toLowerCase());

        return Map.of(
            "deletedAdmins", deletedAdmins,
            "deletedChallenges", deletedChallenges,
            "deletedPeople", deletedPeople,
            "deletedOrganizations", deletedOrganizations
        );
    }

    @GetMapping("/verification-code")
    public Map<String, String> getLatestVerificationCode(@RequestParam String email) {
        String code = devVerificationInbox.getLatestCode(email.trim().toLowerCase());
        return Map.of("code", code == null ? "" : code);
    }

    @PostMapping("/dns-txt")
    public Map<String, String> putDnsTxtRecord(@RequestParam String domain, @RequestParam String value) {
        devDnsTxtStore.put(domain.trim().toLowerCase(), value);
        return Map.of("domain", domain.trim().toLowerCase(), "value", value);
    }

    @DeleteMapping("/account")
    @Transactional
    public Map<String, Long> deleteAccountData(@RequestParam String email) {
        String normalizedEmail = email.trim().toLowerCase();
        long deletedPeople = personRepository.deleteByEmailIgnoreCase(normalizedEmail);
        long deletedAdmins = organizationAdminRepository.deleteByAccountEmail(normalizedEmail);
        long deletedCodes = emailVerificationCodeRepository.deleteByEmail(normalizedEmail);
        long deletedChallenges = domainVerificationChallengeRepository.deleteByRequestedByAccountEmail(normalizedEmail);
        long deletedAccounts = accountRepository.deleteByEmail(normalizedEmail);
        devVerificationInbox.remove(normalizedEmail);

        return Map.of(
            "deletedPeople", deletedPeople,
            "deletedAdmins", deletedAdmins,
            "deletedCodes", deletedCodes,
            "deletedChallenges", deletedChallenges,
            "deletedAccounts", deletedAccounts
        );
    }

    @DeleteMapping("/dns-txt")
    public Map<String, String> deleteDnsTxtRecord(@RequestParam String domain) {
        String normalizedDomain = domain.trim().toLowerCase();
        devDnsTxtStore.remove(normalizedDomain);
        return Map.of("domain", normalizedDomain);
    }
}
