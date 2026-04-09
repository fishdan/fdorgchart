package com.fishdan.myorgchart;

import com.fishdan.myorgchart.account.Account;
import com.fishdan.myorgchart.account.AccountMembershipService;
import com.fishdan.myorgchart.account.AccountService;
import com.fishdan.myorgchart.account.DomainVerificationService;
import com.fishdan.myorgchart.account.OrganizationAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    private final AccountService accountService;
    private final AccountMembershipService accountMembershipService;
    private final DomainVerificationService domainVerificationService;
    private final OrganizationAdminService organizationAdminService;

    public PageController(
        AccountService accountService,
        AccountMembershipService accountMembershipService,
        DomainVerificationService domainVerificationService,
        OrganizationAdminService organizationAdminService
    ) {
        this.accountService = accountService;
        this.accountMembershipService = accountMembershipService;
        this.domainVerificationService = domainVerificationService;
        this.organizationAdminService = organizationAdminService;
    }

    @GetMapping("/")
    public String homePage(
        @RequestParam(required = false) Boolean verified,
        @RequestParam(required = false) Boolean login,
        HttpSession session,
        Model model
    ) {
        model.addAttribute("accountEmail", session.getAttribute("accountEmail"));
        model.addAttribute("verified", Boolean.TRUE.equals(verified));
        model.addAttribute("login", Boolean.TRUE.equals(login));
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

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/verify-account")
    public String verifyAccountPage(
        @RequestParam(required = false) Boolean sent,
        @RequestParam(required = false) Boolean login,
        HttpSession session,
        Model model
    ) {
        Object accountEmail = session.getAttribute("accountEmail");
        if (accountEmail == null) {
            return "redirect:/login";
        }

        model.addAttribute("accountEmail", accountEmail);
        model.addAttribute("sent", Boolean.TRUE.equals(sent));
        model.addAttribute("login", Boolean.TRUE.equals(login));
        return "verify-account";
    }

    @GetMapping("/settings")
    public String settingsPage(
        @RequestParam(required = false) Boolean passwordChanged,
        @RequestParam(required = false) Boolean membershipAdded,
        @RequestParam(required = false) Boolean membershipUpdated,
        @RequestParam(required = false) Boolean challengeCreated,
        @RequestParam(required = false) Boolean domainVerified,
        @RequestParam(required = false) Boolean membershipApproved,
        @RequestParam(required = false) Boolean membershipRejected,
        @RequestParam(required = false) Boolean adminGranted,
        @RequestParam(required = false) Boolean adminRevoked,
        @RequestParam(required = false) Boolean chartVisibilityUpdated,
        HttpSession session,
        Model model
    ) {
        Object accountId = session.getAttribute("accountId");
        Object accountEmail = session.getAttribute("accountEmail");
        if (accountId == null || accountEmail == null) {
            return "redirect:/login";
        }

        Account account = accountService.getAccountById((Long) accountId);
        model.addAttribute("accountEmail", accountEmail);
        model.addAttribute("verifiedAccount", account.isVerified());
        model.addAttribute("memberships", accountMembershipService.getMemberships(account.getId()));
        model.addAttribute("domainChallenges", domainVerificationService.getChallengesForAccount(account.getId()));
        model.addAttribute("adminOrganizations", domainVerificationService.getAdminOrganizations(account.getId()));
        model.addAttribute("managedOrganizations", organizationAdminService.getManagedOrganizations(account.getId()));
        model.addAttribute("passwordChanged", Boolean.TRUE.equals(passwordChanged));
        model.addAttribute("membershipAdded", Boolean.TRUE.equals(membershipAdded));
        model.addAttribute("membershipUpdated", Boolean.TRUE.equals(membershipUpdated));
        model.addAttribute("challengeCreated", Boolean.TRUE.equals(challengeCreated));
        model.addAttribute("domainVerified", Boolean.TRUE.equals(domainVerified));
        model.addAttribute("membershipApproved", Boolean.TRUE.equals(membershipApproved));
        model.addAttribute("membershipRejected", Boolean.TRUE.equals(membershipRejected));
        model.addAttribute("adminGranted", Boolean.TRUE.equals(adminGranted));
        model.addAttribute("adminRevoked", Boolean.TRUE.equals(adminRevoked));
        model.addAttribute("chartVisibilityUpdated", Boolean.TRUE.equals(chartVisibilityUpdated));
        return "settings";
    }


    @GetMapping("/view-orgchart")
    public String viewOrgChartPage(Model model) {
        model.addAttribute("message", "View the Organization Chart");
        return "orgchart"; // Points to orgchart.html in templates
    }
}
