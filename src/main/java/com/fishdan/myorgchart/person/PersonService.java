package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;

    public PersonService(PersonRepository personRepository, OrganizationRepository organizationRepository) {
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
    }

    public Person createPerson(Person person) {
        // Ensure the organization domain exists
        Organization organization = organizationRepository.findByDomain(person.getDomain());
        if (organization == null) {
            throw new IllegalArgumentException("No organization exists with the domain: " + person.getDomain());
        }
        person.setOrganization(organization);

        // Check for uniqueness of email + domain
        if (personRepository.existsByEmailAndDomain(person.getEmail(), person.getDomain())) {
            throw new IllegalArgumentException("A person with this email and domain already exists.");
        }

        return personRepository.save(person);
    }
}
