package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonServiceTest {

    @Test
    void createPersonNormalizesBlankSupervisorEmailToNull() {
        PersonRepository personRepository = mock(PersonRepository.class);
        OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
        PersonService personService = new PersonService(personRepository, organizationRepository);

        Organization organization = new Organization();
        Person person = new Person();
        person.setFullName("Daniel Fishman");
        person.setEmail("dan@fishdan.com");
        person.setDomain("fishdan.com");
        person.setDepartment("CEO");
        person.setSupervisorEmail("");

        when(organizationRepository.findByDomain("fishdan.com")).thenReturn(organization);
        when(personRepository.existsByEmailAndDomain("dan@fishdan.com", "fishdan.com")).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personService.createPerson(person);

        ArgumentCaptor<Person> savedPerson = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(savedPerson.capture());
        assertNull(savedPerson.getValue().getSupervisorEmail());
    }
}
