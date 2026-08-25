package com.domainreg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Simple DNS TXT record lookup for domain ownership verification.
 */
@Component
public class DnsLookup {

    private static final Logger log = LoggerFactory.getLogger(DnsLookup.class);

    /**
     * Look up TXT records for the given hostname.
     * Returns empty list if no TXT records or lookup fails.
     */
    public List<String> lookupTxt(String hostname) {
        List<String> records = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            // Query system DNS (or /etc/resolv.conf); fall back to Google DNS
            env.put("java.naming.provider.url", "dns://8.8.8.8");

            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(hostname, new String[]{"TXT"});
            Attribute txt = attrs.get("TXT");
            if (txt != null) {
                for (int i = 0; i < txt.size(); i++) {
                    String value = (String) txt.get(i);
                    // Strip surrounding quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    records.add(value);
                }
            }
            ctx.close();
        } catch (Exception e) {
            log.warn("DNS TXT lookup failed for {}: {}", hostname, e.getMessage());
        }
        return records;
    }

    /**
     * Look up A records for the given hostname (used by DNSBL reputation checks).
     * Returns empty list if no A records or lookup fails.
     */
    public List<String> lookupA(String hostname) {
        List<String> results = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns://8.8.8.8");

            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(hostname, new String[]{"A"});
            Attribute a = attrs.get("A");
            if (a != null) {
                for (int i = 0; i < a.size(); i++) {
                    results.add(String.valueOf(a.get(i)));
                }
            }
            ctx.close();
        } catch (Exception e) {
            log.warn("DNS A lookup failed for {}: {}", hostname, e.getMessage());
        }
        return results;
    }
}
