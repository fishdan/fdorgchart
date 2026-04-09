package com.fishdan.myorgchart.person;

import com.fishdan.myorgchart.account.AccountSession;
import jakarta.servlet.http.HttpSession;
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
    public String createPersonFromForm(Person person, Model model, HttpSession session) {
        return createPerson(person, model, session);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public String createPersonFromJson(@RequestBody Person person, Model model, HttpSession session) {
        return createPerson(person, model, session);
    }

    private String createPerson(Person person, Model model, HttpSession session) {
        try {
            String authenticatedEmail = (String) session.getAttribute(AccountSession.ACCOUNT_EMAIL);
            personService.createPerson(person, authenticatedEmail);
            return "redirect:/create-person?success=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "person";
        }
    }
}
