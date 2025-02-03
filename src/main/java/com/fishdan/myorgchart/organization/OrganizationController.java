package com.fishdan.myorgchart.organization;

import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

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

    @GetMapping("/{id}")
    public Organization getOrganizationById(@PathVariable Long id) {
        return organizationService.getOrganizationById(id);
    }
}
