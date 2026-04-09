package com.fishdan.myorgchart.account;

public interface VerificationEmailSender {
    void sendVerificationEmail(String toEmail, String code);
}
