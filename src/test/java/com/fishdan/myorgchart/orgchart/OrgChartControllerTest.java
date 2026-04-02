package com.fishdan.myorgchart.orgchart;

import com.fishdan.myorgchart.organization.OrganizationRepository;
import com.fishdan.myorgchart.person.Person;
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

    @Test
    void getOrgChartTreatsBlankSupervisorEmailAsTopLevel() throws Exception {
        Person person = new Person();
        person.setFullName("Daniel Fishman");
        person.setEmail("dan@fishdan.com");
        person.setDepartment("CEO");
        person.setSupervisorEmail("");

        when(organizationRepository.existsByDomain("fishdan.com")).thenReturn(true);
        when(personRepository.findByDomain("fishdan.com")).thenReturn(List.of(person));

        mockMvc.perform(get("/api/orgchart").param("domain", "fishdan.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].email").value("dan@fishdan.com"));
    }
}
