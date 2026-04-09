package com.fishdan.myorgchart.orgchart;

import com.fishdan.myorgchart.account.AccountSession;
import com.fishdan.myorgchart.account.OrganizationAdminRepository;
import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonApprovalStatus;
import com.fishdan.myorgchart.person.PersonRepository;
import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orgchart")
public class OrgChartController {

    private final OrganizationRepository organizationRepository;
    private final PersonRepository personRepository;
    private final OrganizationAdminRepository organizationAdminRepository;

    public OrgChartController(
        OrganizationRepository organizationRepository,
        PersonRepository personRepository,
        OrganizationAdminRepository organizationAdminRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.personRepository = personRepository;
        this.organizationAdminRepository = organizationAdminRepository;
    }

    @GetMapping
    public ResponseEntity<?> getOrgChart(@RequestParam(required = false) String domain, HttpSession session) {
        if (domain != null && !domain.isBlank()) {
            Organization organization = organizationRepository.findByDomain(domain);
            if (organization != null) {
                if (organization.isPrivateChart() && !canViewPrivateChart(organization, session)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Organization '" + domain + "' chart is private."));
                }

                List<Person> people = organization.isOfficial()
                    ? personRepository.findByDomainAndApprovalStatusIgnoreCase(domain, PersonApprovalStatus.APPROVED)
                    : personRepository.findByDomain(domain);
                Map<String, List<Person>> groupedBySupervisor = people.stream()
                    .collect(Collectors.groupingBy(person ->
                        person.getSupervisorEmail() != null && !person.getSupervisorEmail().isBlank()
                            ? person.getSupervisorEmail()
                            : "Top Level"));


                List<Person> topLevel = groupedBySupervisor.get("Top Level");
                List<Map<String, Object>> tree = buildTree(topLevel, groupedBySupervisor);

                return ResponseEntity.ok(tree);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Organization with domain '" + domain + "' does not exist."));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Please provide an organization domain."));
        }
    }

    private boolean canViewPrivateChart(Organization organization, HttpSession session) {
        Object accountId = session.getAttribute(AccountSession.ACCOUNT_ID);
        Object accountEmail = session.getAttribute(AccountSession.ACCOUNT_EMAIL);
        if (accountId instanceof Long accountIdValue
            && organizationAdminRepository.existsByOrganizationIdAndAccountId(organization.getId(), accountIdValue)) {
            return true;
        }
        if (accountEmail instanceof String accountEmailValue) {
            return personRepository.findByEmailAndDomainIgnoreCase(accountEmailValue, organization.getDomain())
                .map(person -> person.getApprovalStatus() == PersonApprovalStatus.APPROVED)
                .orElse(false);
        }
        return false;
    }

    private List<Map<String, Object>> buildTree(List<Person> supervisors, Map<String, List<Person>> groupedBySupervisor) {
        List<Map<String, Object>> tree = new ArrayList<>();
        if (supervisors != null) {
            for (Person supervisor : supervisors) {
                if (supervisor != null) {
                    Map<String, Object> node = new HashMap<>();
                    node.put("name", supervisor.getFullName() != null ? supervisor.getFullName() : "Unknown");
                    node.put("email", supervisor.getEmail() != null ? supervisor.getEmail() : "No Email");
                    node.put("department", supervisor.getDepartment() != null ? supervisor.getDepartment() : "No Department");
                    List<Person> subordinates = groupedBySupervisor.get(supervisor.getEmail());
                    node.put("children", subordinates != null ? buildTree(subordinates, groupedBySupervisor) : Collections.emptyList());
                    tree.add(node);
                }
            }
        }
        return tree;
    }

}
