package com.fishdan.myorgchart.account;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class DevVerificationEmailSender implements VerificationEmailSender {

    private final DevVerificationInbox devVerificationInbox;

    public DevVerificationEmailSender(DevVerificationInbox devVerificationInbox) {
        this.devVerificationInbox = devVerificationInbox;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String code) {
        devVerificationInbox.putCode(toEmail, code);
    }
}
