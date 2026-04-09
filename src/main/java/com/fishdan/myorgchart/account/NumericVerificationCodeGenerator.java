package com.fishdan.myorgchart.account;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class NumericVerificationCodeGenerator implements VerificationCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
