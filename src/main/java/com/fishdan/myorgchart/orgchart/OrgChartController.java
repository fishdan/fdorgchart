package com.fishdan.myorgchart.orgchart;

import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonRepository;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orgchart")
public class OrgChartController {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PersonRepository personRepository;

    @GetMapping
    public ResponseEntity<?> getOrgChart(@RequestParam(required = false) String domain) {
        if (domain != null && !domain.isBlank()) {
            boolean orgExists = organizationRepository.existsByDomain(domain);
            if (orgExists) {
                List<Person> people = personRepository.findByDomain(domain);
                Map<String, List<Person>> groupedBySupervisor = people.stream()
                    .collect(Collectors.groupingBy(person ->
                        person.getSupervisorEmail() != null ? person.getSupervisorEmail() : "Top Level"));


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

