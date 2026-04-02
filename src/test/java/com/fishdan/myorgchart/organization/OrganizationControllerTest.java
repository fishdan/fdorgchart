package com.fishdan.myorgchart.organization;

import com.fishdan.myorgchart.person.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private OrganizationRepository organizationRepository;

    @MockBean
    private PersonRepository personRepository;

    @Test
    void createOrganizationAcceptsJsonPosts() throws Exception {
        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Fishdan",
                      "domain": "fishdan.com",
                      "email": "admin@fishdan.com",
                      "password": "plain-password"
                    }
                    """))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/create-organization?success=true"));

        verify(organizationService).createOrganization(any(Organization.class));
    }

    @Test
    void createOrganizationRendersFormWithErrorWhenServiceRejectsInput() throws Exception {
        doThrow(new IllegalArgumentException("Domain already exists. Please choose another one."))
            .when(organizationService).createOrganization(any(Organization.class));

        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Fishdan",
                      "domain": "fishdan.com",
                      "email": "admin@fishdan.com",
                      "password": "plain-password"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(view().name("organization"))
            .andExpect(model().attribute("error",
                "Domain already exists. Please choose another one."));
    }
}
