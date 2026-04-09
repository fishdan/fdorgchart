package com.fishdan.myorgchart.account;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    static final Duration VERIFICATION_CODE_TTL = Duration.ofMinutes(15);
    static final Duration RESEND_WINDOW = Duration.ofMinutes(5);
    static final Duration DAILY_CAP_WINDOW = Duration.ofHours(24);
    static final int MAX_SENDS_PER_WINDOW = 5;

    private final AccountRepository accountRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationEmailSender verificationEmailSender;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final Clock clock;

    public AccountService(
        AccountRepository accountRepository,
        EmailVerificationCodeRepository emailVerificationCodeRepository,
        PasswordEncoder passwordEncoder,
        VerificationEmailSender verificationEmailSender,
        VerificationCodeGenerator verificationCodeGenerator,
        Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationEmailSender = verificationEmailSender;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.clock = clock;
    }

    @Transactional
    public Account registerAccount(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account already exists for that email address.");
        }

        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setCreatedAt(clock.instant());
        Account savedAccount = accountRepository.save(account);

        sendVerificationCode(savedAccount);
        return savedAccount;
    }

    @Transactional
    public Account authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Account account = accountRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        account.setLastLoginAt(clock.instant());
        return accountRepository.save(account);
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    public Account getVerifiedAccountByEmail(String email) {
        return accountRepository.findByEmail(normalizeEmail(email))
            .filter(Account::isVerified)
            .orElse(null);
    }

    @Transactional
    public void changePassword(Long accountId, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }

        Account account = getAccountById(accountId);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    @Transactional
    public void sendVerificationCode(Long accountId) {
        Account account = getAccountById(accountId);
        sendVerificationCode(account);
    }

    private void sendVerificationCode(Account account) {
        if (account.isVerified()) {
            throw new IllegalArgumentException("This email address is already verified.");
        }

        Instant now = clock.instant();
        emailVerificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(account.getEmail())
            .ifPresent(latest -> {
                if (latest.getCreatedAt().plus(RESEND_WINDOW).isAfter(now)) {
                    throw new IllegalArgumentException("Please wait 5 minutes before requesting another code.");
                }
            });

        Instant dailyCutoff = now.minus(DAILY_CAP_WINDOW);
        long sendsInWindow = emailVerificationCodeRepository.countByEmailAndCreatedAtAfter(
            account.getEmail(),
            dailyCutoff
        );
        if (sendsInWindow >= MAX_SENDS_PER_WINDOW) {
            throw new IllegalArgumentException("This email address has already reached the daily verification-send limit.");
        }

        List<EmailVerificationCode> pendingCodes =
            emailVerificationCodeRepository.findByAccountIdAndStatusOrderByCreatedAtDesc(
                account.getId(),
                EmailVerificationCodeStatus.PENDING
            );
        if (!pendingCodes.isEmpty()) {
            for (EmailVerificationCode pendingCode : pendingCodes) {
                pendingCode.setStatus(EmailVerificationCodeStatus.INVALIDATED);
            }
            emailVerificationCodeRepository.saveAll(pendingCodes);
        }

        String rawCode = verificationCodeGenerator.generateCode();
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setAccount(account);
        verificationCode.setEmail(account.getEmail());
        verificationCode.setCodeHash(passwordEncoder.encode(rawCode));
        verificationCode.setStatus(EmailVerificationCodeStatus.PENDING);
        verificationCode.setCreatedAt(now);
        verificationCode.setExpiresAt(now.plus(VERIFICATION_CODE_TTL));
        emailVerificationCodeRepository.save(verificationCode);

        verificationEmailSender.sendVerificationEmail(account.getEmail(), rawCode);
    }

    @Transactional
    public void confirmVerificationCode(Long accountId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Verification code is required.");
        }

        EmailVerificationCode verificationCode = emailVerificationCodeRepository
            .findFirstByAccountIdAndStatusOrderByCreatedAtDesc(accountId, EmailVerificationCodeStatus.PENDING)
            .orElseThrow(() -> new IllegalArgumentException("No active verification code. Request a new code."));

        Instant now = clock.instant();
        if (verificationCode.getExpiresAt().isBefore(now)) {
            verificationCode.setStatus(EmailVerificationCodeStatus.EXPIRED);
            emailVerificationCodeRepository.save(verificationCode);
            throw new IllegalArgumentException("Verification code expired. Request a new code.");
        }

        if (!passwordEncoder.matches(code.trim(), verificationCode.getCodeHash())) {
            throw new IllegalArgumentException("Verification code is invalid.");
        }

        verificationCode.setStatus(EmailVerificationCodeStatus.CONSUMED);
        verificationCode.setConsumedAt(now);
        emailVerificationCodeRepository.save(verificationCode);

        Account account = verificationCode.getAccount();
        account.setEmailVerifiedAt(now);
        accountRepository.save(account);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        return email.trim().toLowerCase();
    }
}
