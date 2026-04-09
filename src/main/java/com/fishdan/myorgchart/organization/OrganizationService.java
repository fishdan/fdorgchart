package com.fishdan.myorgchart.organization;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(
        OrganizationRepository organizationRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public Organization createOrganization(Organization organization) {
        String normalizedDomain = organization.getDomain().trim().toLowerCase(Locale.ROOT);
        if (organizationRepository.existsByDomain(normalizedDomain)) {
            throw new IllegalArgumentException("Domain already exists. Please choose another one.");
        }
        organization.setDomain(normalizedDomain);
        organization.setPassword(passwordEncoder.encode(organization.getPassword()));
        organization.setOwnershipType(OrganizationOwnership.OPEN);
        return organizationRepository.save(organization);
    }

    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id).orElseThrow(() ->
            new RuntimeException("Organization not found"));
    }
}
