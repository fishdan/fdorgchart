package com.fishdan.myorgchart.account;

import java.util.List;

public interface DnsTxtLookup {
    List<String> lookupTxtRecords(String domain);
}
