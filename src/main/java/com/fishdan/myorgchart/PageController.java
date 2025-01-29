package com.fishdan.myorgchart;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/create-organization")
    public String createOrganizationPage(Model model) {
        model.addAttribute("message", "Create a New Organization");
        return "organization"; // Points to organization.html in templates
    }

    @GetMapping("/hello")
    public String helloPage() {
        return "hello";
    }


    @GetMapping("/create-person")
    public String createPersonPage(Model model) {
        model.addAttribute("message", "Create a New Person");
        return "person"; // Points to person.html in templates
    }

    @GetMapping("/view-org-chart")
    public String viewOrgChartPage(Model model) {
        model.addAttribute("message", "View the Organization Chart");
        return "org-chart"; // Points to org-chart.html in templates
    }
}
