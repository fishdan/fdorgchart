package com.fishdan.myorgchart.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountMembershipService accountMembershipService;

    @MockBean
    private DomainVerificationService domainVerificationService;

    @MockBean
    private OrganizationAdminService organizationAdminService;

    @Test
    void registerCreatesAccountAndRedirectsToVerification() throws Exception {
        Account account = new Account();
        account.setId(42L);
        account.setEmail("person@example.com");

        when(accountService.registerAccount("person@example.com", "plain-password")).thenReturn(account);

        mockMvc.perform(post("/account/register")
                .param("email", "person@example.com")
                .param("password", "plain-password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/verify-account?sent=true"))
            .andExpect(request().sessionAttribute(AccountSession.ACCOUNT_ID, 42L))
            .andExpect(request().sessionAttribute(AccountSession.ACCOUNT_EMAIL, "person@example.com"));
    }

    @Test
    void loginRedirectsVerifiedAccountHome() throws Exception {
        Account account = new Account();
        account.setId(7L);
        account.setEmail("verified@example.com");
        account.setEmailVerifiedAt(java.time.Instant.parse("2026-04-09T10:00:00Z"));

        when(accountService.authenticate("verified@example.com", "secret")).thenReturn(account);

        mockMvc.perform(post("/account/login")
                .param("email", "verified@example.com")
                .param("password", "secret"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/?login=true"));
    }

    @Test
    void loginRedirectsUnverifiedAccountToVerification() throws Exception {
        Account account = new Account();
        account.setId(8L);
        account.setEmail("pending@example.com");

        when(accountService.authenticate("pending@example.com", "secret")).thenReturn(account);

        mockMvc.perform(post("/account/login")
                .param("email", "pending@example.com")
                .param("password", "secret"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/verify-account?login=true"));
    }

    @Test
    void confirmVerificationRedirectsHomeOnSuccess() throws Exception {
        doNothing().when(accountService).confirmVerificationCode(42L, "123456");

        mockMvc.perform(post("/account/verification/confirm")
                .sessionAttr(AccountSession.ACCOUNT_ID, 42L)
                .sessionAttr(AccountSession.ACCOUNT_EMAIL, "person@example.com")
                .param("code", "123456"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/?verified=true"));
    }

    @Test
    void resendVerificationCodeRendersVerifyPageWhenThrottled() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Please wait 5 minutes before requesting another code."))
            .when(accountService).sendVerificationCode(42L);

        mockMvc.perform(post("/account/verification/send")
                .sessionAttr(AccountSession.ACCOUNT_ID, 42L)
                .sessionAttr(AccountSession.ACCOUNT_EMAIL, "person@example.com"))
            .andExpect(status().isOk())
            .andExpect(view().name("verify-account"))
            .andExpect(model().attribute("accountEmail", "person@example.com"))
            .andExpect(model().attribute("error", "Please wait 5 minutes before requesting another code."));
    }

    @Test
    void addMembershipRedirectsToSettingsOnSuccess() throws Exception {
        mockMvc.perform(post("/account/memberships")
                .sessionAttr(AccountSession.ACCOUNT_ID, 42L)
                .param("fullName", "Person Example")
                .param("domain", "fishdan.com")
                .param("department", "Engineering"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings?membershipAdded=true"));
    }

    @Test
    void startDomainChallengeRedirectsToSettingsOnSuccess() throws Exception {
        mockMvc.perform(post("/account/domains/challenges")
                .sessionAttr(AccountSession.ACCOUNT_ID, 42L)
                .param("domain", "fishdan.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings?challengeCreated=true"));
    }

    @Test
    void approveMembershipRedirectsToSettingsOnSuccess() throws Exception {
        mockMvc.perform(post("/account/admin/memberships/approve")
                .sessionAttr(AccountSession.ACCOUNT_ID, 42L)
                .param("personId", "9"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings?membershipApproved=true"));
    }

    @Test
    void updateChartVisibilityRedirectsToSettingsOnSuccess() throws Exception {
        mockMvc.perform(post("/account/admin/chart-visibility")
                .sessionAttr(AccountSession.ACCOUNT_ID, 42L)
                .param("organizationId", "3")
                .param("visibility", "PRIVATE"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/settings?chartVisibilityUpdated=true"));
    }
}
