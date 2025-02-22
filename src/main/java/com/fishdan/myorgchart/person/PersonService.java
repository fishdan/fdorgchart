package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.organization.Organization;
import com.fishdan.myorgchart.organization.OrganizationRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;

    public PersonService(PersonRepository personRepository, OrganizationRepository organizationRepository) {
        this.personRepository = personRepository;
        this.organizationRepository = organizationRepository;
    }

    public Person createPerson(Person person) {
        // Normalize domain to lowercase
        String normalizedDomain = person.getDomain().toLowerCase();

        // Ensure the organization domain exists in the database
        Organization organization = organizationRepository.findByDomain(normalizedDomain);
        if (organization == null) {
            throw new IllegalArgumentException("No organization exists with the domain: " + person.getDomain());
        }

        person.setOrganization(organization);
        person.setDomain(normalizedDomain); // Store in lowercase for consistency

        // Check for uniqueness of email + domain
        if (personRepository.existsByEmailAndDomain(person.getEmail(), normalizedDomain)) {
            throw new IllegalArgumentException("A person with this email and domain already exists.");
        }

        return personRepository.save(person);
    }

}
