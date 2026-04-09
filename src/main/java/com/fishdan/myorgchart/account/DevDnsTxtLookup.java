package com.fishdan.myorgchart.account;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
public class DevDnsTxtLookup implements DnsTxtLookup {

    private final DevDnsTxtStore devDnsTxtStore;

    public DevDnsTxtLookup(DevDnsTxtStore devDnsTxtStore) {
        this.devDnsTxtStore = devDnsTxtStore;
    }

    @Override
    public List<String> lookupTxtRecords(String domain) {
        return devDnsTxtStore.get(domain);
    }
}
