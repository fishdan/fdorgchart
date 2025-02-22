package com.fishdan.myorgchart.orgchart;

import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonRepository;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class OrgChartController {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PersonRepository personRepository;

    @GetMapping("/orgchart")
    public String showOrgChartPage(@RequestParam(required = false) String domain, Model model) {
        if (domain != null && !domain.isBlank()) {
            boolean orgExists = organizationRepository.existsByDomain(domain);
            if (orgExists) {
                List<Person> people = personRepository.findByDomain(domain);
                Map<String, List<Person>> groupedBySupervisor = people.stream()
                    .collect(Collectors.groupingBy(Person::getSupervisorEmail));

                List<Person> topLevel = groupedBySupervisor.get(null);
                model.addAttribute("peopleTree", buildTree(topLevel, groupedBySupervisor));
                model.addAttribute("domain", domain);
            } else {
                model.addAttribute("error", "Organization with domain '" + domain + "' does not exist.");
            }
        } else {
            // No domain specified, show a prompt
            model.addAttribute("error", "Please enter an organization domain to see the org chart.");
            model.addAttribute("peopleTree", Collections.emptyList());
        }
        return "orgchart";
    }


    private List<Map<String, Object>> buildTree(List<Person> supervisors, Map<String, List<Person>> groupedBySupervisor) {
        List<Map<String, Object>> tree = new ArrayList<>();
        if (supervisors != null) {
            for (Person supervisor : supervisors) {
                Map<String, Object> node = new HashMap<>();
                node.put("name", supervisor.getFullName());
                node.put("email", supervisor.getEmail());
                List<Person> subordinates = groupedBySupervisor.get(supervisor.getEmail());
                node.put("children", buildTree(subordinates, groupedBySupervisor));
                tree.add(node);
            }
        }
        return tree;
    }
}
