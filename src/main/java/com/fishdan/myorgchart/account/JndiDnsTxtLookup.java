package com.fishdan.myorgchart.account;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Component
@Profile("!dev")
public class JndiDnsTxtLookup implements DnsTxtLookup {

    @Override
    public List<String> lookupTxtRecords(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            DirContext context = new InitialDirContext(env);
            Attributes attributes = context.getAttributes(domain, new String[] {"TXT"});
            Attribute txtAttribute = attributes.get("TXT");
            List<String> values = new ArrayList<>();
            if (txtAttribute == null) {
                return values;
            }

            NamingEnumeration<?> records = txtAttribute.getAll();
            while (records.hasMore()) {
                String record = String.valueOf(records.next());
                values.add(record.replace("\"", ""));
            }
            return values;
        } catch (NamingException e) {
            return List.of();
        }
    }
}
