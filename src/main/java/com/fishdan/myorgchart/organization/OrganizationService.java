package com.fishdan.myorgchart.organization;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public Organization createOrganization(Organization organization) {
        if (organizationRepository.existsByDomain(organization.getDomain())) {
            throw new IllegalArgumentException("Domain already exists. Please choose another one.");
        }
        return organizationRepository.save(organization);
    }

    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id).orElseThrow(() ->
            new RuntimeException("Organization not found"));
    }
}

