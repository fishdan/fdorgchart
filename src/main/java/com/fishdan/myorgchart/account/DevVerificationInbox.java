package com.fishdan.myorgchart.account;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class DevVerificationInbox {

    private final Map<String, String> latestCodesByEmail = new ConcurrentHashMap<>();

    public void putCode(String email, String code) {
        latestCodesByEmail.put(email, code);
    }

    public String getLatestCode(String email) {
        return latestCodesByEmail.get(email);
    }

    public void remove(String email) {
        latestCodesByEmail.remove(email);
    }
}
