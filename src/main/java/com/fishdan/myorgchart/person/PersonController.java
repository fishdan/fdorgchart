package com.fishdan.myorgchart.person;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String createPersonFromForm(Person person, Model model) {
        return createPerson(person, model);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public String createPersonFromJson(@RequestBody Person person, Model model) {
        return createPerson(person, model);
    }

    private String createPerson(Person person, Model model) {
        try {
            personService.createPerson(person);
            return "redirect:/create-person?success=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "person";
        }
    }
}
