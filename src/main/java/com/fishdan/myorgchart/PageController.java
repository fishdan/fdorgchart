package com.fishdan.myorgchart;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @GetMapping("/")
    public String homePage() {
        return "index"; // Loads index.html
    }

    @GetMapping("/create-organization")
    public String createOrganizationPage(@RequestParam(required = false) Boolean success, Model model) {
        model.addAttribute("message", "Create a New Organization");
        model.addAttribute("success", Boolean.TRUE.equals(success));
        return "organization"; // Points to organization.html in templates
    }


    @GetMapping("/orgchart")
    public String orgChartPage() {
        return "orgchart";
    }

    @GetMapping("/create-person")
    public String createPersonPage(@RequestParam(required = false) Boolean success, Model model) {
        model.addAttribute("success", Boolean.TRUE.equals(success));
        return "person";
    }


    @GetMapping("/view-orgchart")
    public String viewOrgChartPage(Model model) {
        model.addAttribute("message", "View the Organization Chart");
        return "orgchart"; // Points to orgchart.html in templates
    }
}
