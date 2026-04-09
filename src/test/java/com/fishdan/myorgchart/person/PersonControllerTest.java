package com.fishdan.myorgchart.person;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PersonService personService;

    @Test
    void createPersonAcceptsHtmlFormPosts() throws Exception {
        mockMvc.perform(post("/api/people")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@fishdan.com")
                .param("domain", "fishdan.com")
                .param("department", "Engineering")
                .param("supervisorEmail", "boss@fishdan.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/create-person?success=true"));

        verify(personService).createPerson(any(Person.class), isNull());
    }

    @Test
    void createPersonRendersFormWithErrorWhenServiceRejectsInput() throws Exception {
        doThrow(new IllegalArgumentException("No organization exists with the domain: fishdan.com"))
            .when(personService).createPerson(any(Person.class), isNull());

        mockMvc.perform(post("/api/people")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@fishdan.com")
                .param("domain", "fishdan.com")
                .param("department", "Engineering"))
            .andExpect(status().isOk())
            .andExpect(view().name("person"))
            .andExpect(model().attribute("error", "No organization exists with the domain: fishdan.com"));
    }

    @Test
    void createPersonShowsPrivateAccountMessageWhenEmailIsReserved() throws Exception {
        doThrow(new IllegalArgumentException(
            "reserved@example.com has a private account. Please send an email to reserved@example.com asking them to add themselves to Fishdan"
        )).when(personService).createPerson(any(Person.class), isNull());

        mockMvc.perform(post("/api/people")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Reserved User")
                .param("email", "reserved@example.com")
                .param("domain", "fishdan.com")
                .param("department", "Engineering"))
            .andExpect(status().isOk())
            .andExpect(view().name("person"))
            .andExpect(model().attribute("error",
                "reserved@example.com has a private account. Please send an email to reserved@example.com asking them to add themselves to Fishdan"));
    }
}
