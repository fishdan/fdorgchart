package com.fishdan.myorgchart;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String homePage() {
        return "index"; // Loads index.html
    }

    @GetMapping("/create-organization")
    public String createOrganizationPage(Model model) {
        model.addAttribute("message", "Create a New Organization");
        return "organization"; // Points to organization.html in templates
    }



    @GetMapping("/create-person")
    public String createPersonPage() {
        return "person";
    }


    @GetMapping("/view-orgchart")
    public String viewOrgChartPage(Model model) {
        model.addAttribute("message", "View the Organization Chart");
        return "orgchart"; // Points to orgchart.html in templates
    }
}
