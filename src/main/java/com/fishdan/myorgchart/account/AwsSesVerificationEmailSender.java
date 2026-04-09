package com.fishdan.myorgchart.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Service
@Profile("!dev")
public class AwsSesVerificationEmailSender implements VerificationEmailSender {

    private final SesV2Client sesV2Client;
    private final String fromAddress;
    private final String configurationSet;

    public AwsSesVerificationEmailSender(
        SesV2Client sesV2Client,
        @Value("${app.email.from-address:}") String fromAddress,
        @Value("${app.email.configuration-set:}") String configurationSet
    ) {
        this.sesV2Client = sesV2Client;
        this.fromAddress = fromAddress;
        this.configurationSet = configurationSet;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String code) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("Verification email sending is not configured. Set APP_EMAIL_FROM_ADDRESS.");
        }

        SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
            .fromEmailAddress(fromAddress)
            .destination(Destination.builder().toAddresses(toEmail).build())
            .content(EmailContent.builder()
                .simple(Message.builder()
                    .subject(Content.builder().data("MyOrgChart verification code").build())
                    .body(Body.builder()
                        .text(Content.builder()
                            .data("Your MyOrgChart verification code is " + code + ". It expires in 15 minutes.")
                            .build())
                        .build())
                    .build())
                .build());

        if (configurationSet != null && !configurationSet.isBlank()) {
            requestBuilder.configurationSetName(configurationSet);
        }

        sesV2Client.sendEmail(requestBuilder.build());
    }
}
