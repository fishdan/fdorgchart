package com.fishdan.myorgchart.account;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class DevDnsTxtStore {

    private final Map<String, String> recordsByDomain = new ConcurrentHashMap<>();

    public void put(String domain, String value) {
        recordsByDomain.put(domain, value);
    }

    public List<String> get(String domain) {
        String record = recordsByDomain.get(domain);
        return record == null ? List.of() : List.of(record);
    }

    public void remove(String domain) {
        recordsByDomain.remove(domain);
    }
}
