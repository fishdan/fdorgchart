package com.fishdan.myorgchart.organization;

import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private PersonRepository personRepository;


    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<Organization> getAllOrganizations() {
        return organizationService.getAllOrganizations();
    }

    @PostMapping
    public String createOrganization(@RequestBody Organization organization, Model model) {
        try {
            organizationService.createOrganization(organization);
            return "redirect:/create-organization?success=true"; // Redirect after successful creation
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "organization"; // Return back to the form with an error message
        }
    }

    @GetMapping("/orgchart")
    public String showOrgChartPage(@RequestParam(required = false) String domain, Model model) {
        if (domain != null && !domain.isBlank()) {
            boolean orgExists = organizationRepository.existsByDomain(domain);
            if (orgExists) {
                List<Person> people = personRepository.findByDomain(domain);
                model.addAttribute("people", people);
                model.addAttribute("domain", domain);
            } else {
                model.addAttribute("error", "Organization with domain '" + domain + "' does not exist.");
            }
        }
        return "orgchart";
    }


    @GetMapping("/{id}")
    public Organization getOrganizationById(@PathVariable Long id) {
        return organizationService.getOrganizationById(id);
    }
}
