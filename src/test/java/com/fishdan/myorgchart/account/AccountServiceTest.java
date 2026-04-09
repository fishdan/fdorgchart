package com.fishdan.myorgchart.account;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Test
    void registerAccountHashesPasswordAndSendsVerificationCode() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        EmailVerificationCodeRepository codeRepository = mock(EmailVerificationCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        AccountService accountService = new AccountService(
            accountRepository,
            codeRepository,
            passwordEncoder,
            emailSender,
            codeGenerator,
            clock
        );

        when(accountRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(passwordEncoder.encode("123456")).thenReturn("hashed-code");
        when(codeGenerator.generateCode()).thenReturn("123456");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            if (account.getId() == null) {
                account.setId(11L);
            }
            return account;
        });
        when(codeRepository.countByEmailAndCreatedAtAfter(eq("person@example.com"), any(Instant.class))).thenReturn(0L);
        when(codeRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(11L, EmailVerificationCodeStatus.PENDING))
            .thenReturn(List.of());

        accountService.registerAccount("Person@Example.com", "plain-password");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, atLeastOnce()).save(accountCaptor.capture());
        assertEquals("person@example.com", accountCaptor.getAllValues().get(0).getEmail());
        assertEquals("hashed-password", accountCaptor.getAllValues().get(0).getPasswordHash());

        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(codeRepository).save(codeCaptor.capture());
        assertEquals("person@example.com", codeCaptor.getValue().getEmail());
        assertEquals("hashed-code", codeCaptor.getValue().getCodeHash());
        verify(emailSender).sendVerificationEmail("person@example.com", "123456");
    }

    @Test
    void authenticateRejectsInvalidPassword() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        EmailVerificationCodeRepository codeRepository = mock(EmailVerificationCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        Account account = new Account();
        account.setId(5L);
        account.setEmail("person@example.com");
        account.setPasswordHash("hashed-password");

        when(accountRepository.findByEmail("person@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("bad-password", "hashed-password")).thenReturn(false);

        AccountService accountService = new AccountService(
            accountRepository,
            codeRepository,
            passwordEncoder,
            emailSender,
            codeGenerator,
            clock
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.authenticate("person@example.com", "bad-password")
        );

        assertEquals("Invalid email or password.", exception.getMessage());
    }

    @Test
    void confirmVerificationCodeMarksAccountVerified() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        EmailVerificationCodeRepository codeRepository = mock(EmailVerificationCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        Account account = new Account();
        account.setId(5L);
        account.setEmail("person@example.com");

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setAccount(account);
        verificationCode.setCodeHash("hashed-code");
        verificationCode.setStatus(EmailVerificationCodeStatus.PENDING);
        verificationCode.setCreatedAt(Instant.parse("2026-04-09T11:58:00Z"));
        verificationCode.setExpiresAt(Instant.parse("2026-04-09T12:13:00Z"));

        when(codeRepository.findFirstByAccountIdAndStatusOrderByCreatedAtDesc(5L, EmailVerificationCodeStatus.PENDING))
            .thenReturn(Optional.of(verificationCode));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        AccountService accountService = new AccountService(
            accountRepository,
            codeRepository,
            passwordEncoder,
            emailSender,
            codeGenerator,
            clock
        );

        accountService.confirmVerificationCode(5L, "123456");

        assertNotNull(account.getEmailVerifiedAt());
        assertEquals(EmailVerificationCodeStatus.CONSUMED, verificationCode.getStatus());
        verify(accountRepository).save(account);
        verify(codeRepository).save(verificationCode);
    }

    @Test
    void sendVerificationCodeRejectsRequestsInsideFiveMinuteWindow() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        EmailVerificationCodeRepository codeRepository = mock(EmailVerificationCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        Account account = new Account();
        account.setId(5L);
        account.setEmail("person@example.com");

        EmailVerificationCode latestCode = new EmailVerificationCode();
        latestCode.setCreatedAt(Instant.parse("2026-04-09T11:57:00Z"));

        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(codeRepository.findTopByEmailOrderByCreatedAtDesc("person@example.com"))
            .thenReturn(Optional.of(latestCode));

        AccountService accountService = new AccountService(
            accountRepository,
            codeRepository,
            passwordEncoder,
            emailSender,
            codeGenerator,
            clock
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.sendVerificationCode(5L)
        );

        assertEquals("Please wait 5 minutes before requesting another code.", exception.getMessage());
    }

    @Test
    void sendVerificationCodeRejectsRequestsAfterFiveSendsInTwentyFourHours() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        EmailVerificationCodeRepository codeRepository = mock(EmailVerificationCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        VerificationEmailSender emailSender = mock(VerificationEmailSender.class);
        VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneOffset.UTC);

        Account account = new Account();
        account.setId(5L);
        account.setEmail("person@example.com");

        EmailVerificationCode latestCode = new EmailVerificationCode();
        latestCode.setCreatedAt(Instant.parse("2026-04-09T06:00:00Z"));

        when(accountRepository.findById(5L)).thenReturn(Optional.of(account));
        when(codeRepository.findTopByEmailOrderByCreatedAtDesc("person@example.com"))
            .thenReturn(Optional.of(latestCode));
        when(codeRepository.countByEmailAndCreatedAtAfter(eq("person@example.com"), any(Instant.class)))
            .thenReturn(5L);

        AccountService accountService = new AccountService(
            accountRepository,
            codeRepository,
            passwordEncoder,
            emailSender,
            codeGenerator,
            clock
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.sendVerificationCode(5L)
        );

        assertEquals(
            "This email address has already reached the daily verification-send limit.",
            exception.getMessage()
        );
    }
}
