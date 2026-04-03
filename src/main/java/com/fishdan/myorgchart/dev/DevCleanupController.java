package com.fishdan.myorgchart.dev;

import com.fishdan.myorgchart.organization.OrganizationRepository;
import com.fishdan.myorgchart.person.PersonRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    public DevCleanupController(
        PersonRepository personRepository,
        OrganizationRepository organizationRepository
    ) {
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
    }

    @DeleteMapping("/organization")
    @Transactional
    public Map<String, Long> deleteOrganizationHierarchy(@RequestParam String domain) {
        long deletedPeople = personRepository.deleteByDomainIgnoreCase(domain);
        long deletedOrganizations = organizationRepository.deleteByDomainIgnoreCase(domain);

        return Map.of(
            "deletedPeople", deletedPeople,
            "deletedOrganizations", deletedOrganizations
        );
    }
}
