package com.fishdan.myorgchart.orgchart;

import com.fishdan.myorgchart.account.OrganizationAdminRepository;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationChartVisibility;
import com.fishdan.myorgchart.organization.OrganizationOwnership;
import com.fishdan.myorgchart.person.Person;
import com.fishdan.myorgchart.person.PersonApprovalStatus;
import com.fishdan.myorgchart.person.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrgChartController.class)
class OrgChartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationRepository organizationRepository;

    @MockBean
    private PersonRepository personRepository;

    @MockBean
    private OrganizationAdminRepository organizationAdminRepository;

    @Test
    void getOrgChartTreatsBlankSupervisorEmailAsTopLevel() throws Exception {
        Person person = new Person();
        person.setFullName("Daniel Fishman");
        person.setEmail("dan@fishdan.com");
        person.setDepartment("CEO");
        person.setSupervisorEmail("");

        Organization organization = new Organization();
        organization.setDomain("fishdan.com");
        organization.setOwnershipType(OrganizationOwnership.OPEN);

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);
        when(personRepository.findByDomain("fishdan.com")).thenReturn(List.of(person));

        mockMvc.perform(get("/api/orgchart").param("domain", "fishdan.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].email").value("dan@fishdan.com"));
    }

    @Test
    void getOrgChartHidesProvisionalMembersForOfficialOrganizations() throws Exception {
        Organization organization = new Organization();
        organization.setDomain("fishdan.com");
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);

        Person approved = new Person();
        approved.setFullName("Approved User");
        approved.setEmail("approved@fishdan.com");
        approved.setDepartment("Engineering");
        approved.setApprovalStatus(PersonApprovalStatus.APPROVED);

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);
        when(personRepository.findByDomainAndApprovalStatusIgnoreCase("fishdan.com", PersonApprovalStatus.APPROVED))
            .thenReturn(List.of(approved));

        mockMvc.perform(get("/api/orgchart").param("domain", "fishdan.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].email").value("approved@fishdan.com"));
    }

    @Test
    void getOrgChartRejectsUnauthorizedViewOfPrivateOrganization() throws Exception {
        Organization organization = new Organization();
        organization.setId(4L);
        organization.setDomain("fishdan.com");
        organization.setOwnershipType(OrganizationOwnership.OFFICIAL);
        organization.setChartVisibility(OrganizationChartVisibility.PRIVATE);

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);

        mockMvc.perform(get("/api/orgchart").param("domain", "fishdan.com"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Organization 'fishdan.com' chart is private."));
    }
}
