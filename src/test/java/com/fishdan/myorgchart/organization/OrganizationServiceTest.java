package com.fishdan.myorgchart.organization;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationServiceTest {

    @Test
    void createOrganizationHashesPasswordBeforeSaving() {
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        OrganizationService organizationService =
            new OrganizationService(organizationRepository, passwordEncoder);

        Organization organization = new Organization();
        organization.setName("Fishdan");
        organization.setDomain("fishdan.com");
        organization.setEmail("admin@fishdan.com");
        organization.setPassword("plain-password");

        when(organizationRepository.existsByDomain("fishdan.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(organizationRepository.save(any(Organization.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        organizationService.createOrganization(organization);

        ArgumentCaptor<Organization> savedOrganization =
            ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(savedOrganization.capture());
        assertEquals("hashed-password", savedOrganization.getValue().getPassword());
        assertTrue(savedOrganization.getValue().getPassword() != null);
    }

    @Test
    void createOrganizationRejectsDuplicateDomainBeforeSaving() {
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        OrganizationService organizationService =
            new OrganizationService(organizationRepository, passwordEncoder);

        Organization organization = new Organization();
        organization.setDomain("fishdan.com");
        organization.setPassword("plain-password");

        when(organizationRepository.existsByDomain("fishdan.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> organizationService.createOrganization(organization)
        );

        assertEquals("Domain already exists. Please choose another one.", exception.getMessage());
    }
}
