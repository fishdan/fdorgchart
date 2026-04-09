package com.fishdan.myorgchart.account;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final AccountMembershipService accountMembershipService;
    private final DomainVerificationService domainVerificationService;
    private final OrganizationAdminService organizationAdminService;

    public AccountController(
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

    @PostMapping("/register")
    public String register(
        @RequestParam String email,
        @RequestParam String password,
        HttpSession session,
        Model model
    ) {
        try {
            Account account = accountService.registerAccount(email, password);
            setSession(session, account);
            return "redirect:/verify-account?sent=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @PostMapping("/login")
    public String login(
        @RequestParam String email,
        @RequestParam String password,
        HttpSession session,
        Model model
    ) {
        try {
            Account account = accountService.authenticate(email, password);
            setSession(session, account);
            if (account.isVerified()) {
                return "redirect:/?login=true";
            }
            return "redirect:/verify-account?login=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @PostMapping("/password")
    public String changePassword(
        @RequestParam String currentPassword,
        @RequestParam String newPassword,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            accountService.changePassword(accountId, currentPassword, newPassword);
            return "redirect:/settings?passwordChanged=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("passwordError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/memberships")
    public String addMembership(
        @RequestParam String fullName,
        @RequestParam String domain,
        @RequestParam String department,
        @RequestParam(required = false) String supervisorEmail,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            accountMembershipService.addSelfToOrganization(
                accountId,
                fullName,
                domain,
                department,
                supervisorEmail
            );
            return "redirect:/settings?membershipAdded=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("membershipError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/domains/challenges")
    public String startDomainChallenge(
        @RequestParam String domain,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            domainVerificationService.startChallenge(accountId, domain);
            return "redirect:/settings?challengeCreated=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("domainError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/domains/challenges/verify")
    public String verifyDomainChallenge(
        @RequestParam Long challengeId,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            domainVerificationService.verifyChallenge(accountId, challengeId);
            return "redirect:/settings?domainVerified=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("domainError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/memberships/update")
    public String updateMembership(
        @RequestParam Long personId,
        @RequestParam String department,
        @RequestParam(required = false) String supervisorEmail,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            accountMembershipService.updateOwnMembership(
                accountId,
                personId,
                department,
                supervisorEmail
            );
            return "redirect:/settings?membershipUpdated=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("membershipError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/admin/memberships/approve")
    public String approveMembership(
        @RequestParam Long personId,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            organizationAdminService.approveMembership(accountId, personId);
            return "redirect:/settings?membershipApproved=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("adminError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/admin/memberships/reject")
    public String rejectMembership(
        @RequestParam Long personId,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            organizationAdminService.rejectMembership(accountId, personId);
            return "redirect:/settings?membershipRejected=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("adminError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/admin/admins/grant")
    public String grantAdmin(
        @RequestParam Long personId,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            organizationAdminService.grantAdmin(accountId, personId);
            return "redirect:/settings?adminGranted=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("adminError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/admin/admins/revoke")
    public String revokeAdmin(
        @RequestParam Long personId,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            organizationAdminService.revokeAdmin(accountId, personId);
            return "redirect:/settings?adminRevoked=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("adminError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/admin/chart-visibility")
    public String updateChartVisibility(
        @RequestParam Long organizationId,
        @RequestParam String visibility,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        if (accountId == null) {
            return "redirect:/login";
        }

        try {
            organizationAdminService.updateChartVisibility(accountId, organizationId, "PRIVATE".equalsIgnoreCase(visibility));
            return "redirect:/settings?chartVisibilityUpdated=true";
        } catch (IllegalArgumentException e) {
            populateSettingsModel(accountId, model);
            model.addAttribute("adminError", e.getMessage());
            return "settings";
        }
    }

    @PostMapping("/verification/send")
    public String sendVerificationCode(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        String accountEmail = (String) session.getAttribute(AccountSession.ACCOUNT_EMAIL);
        if (accountId == null || accountEmail == null) {
            return "redirect:/login";
        }

        try {
            accountService.sendVerificationCode(accountId);
            return "redirect:/verify-account?sent=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("accountEmail", accountEmail);
            model.addAttribute("error", e.getMessage());
            return "verify-account";
        }
    }

    @PostMapping("/verification/confirm")
    public String confirmVerificationCode(
        @RequestParam String code,
        HttpSession session,
        Model model
    ) {
        Long accountId = (Long) session.getAttribute(AccountSession.ACCOUNT_ID);
        String accountEmail = (String) session.getAttribute(AccountSession.ACCOUNT_EMAIL);
        if (accountId == null || accountEmail == null) {
            return "redirect:/login";
        }

        try {
            accountService.confirmVerificationCode(accountId, code);
            return "redirect:/?verified=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("accountEmail", accountEmail);
            model.addAttribute("error", e.getMessage());
            return "verify-account";
        }
    }

    private void setSession(HttpSession session, Account account) {
        session.setAttribute(AccountSession.ACCOUNT_ID, account.getId());
        session.setAttribute(AccountSession.ACCOUNT_EMAIL, account.getEmail());
    }

    private void populateSettingsModel(Long accountId, Model model) {
        Account account = accountService.getAccountById(accountId);
        model.addAttribute("accountEmail", account.getEmail());
        model.addAttribute("verifiedAccount", account.isVerified());
        model.addAttribute("memberships", accountMembershipService.getMemberships(accountId));
        model.addAttribute("domainChallenges", domainVerificationService.getChallengesForAccount(accountId));
        model.addAttribute("adminOrganizations", domainVerificationService.getAdminOrganizations(accountId));
        model.addAttribute("managedOrganizations", organizationAdminService.getManagedOrganizations(accountId));
    }
}
