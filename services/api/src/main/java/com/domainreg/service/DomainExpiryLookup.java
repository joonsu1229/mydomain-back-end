package com.domainreg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.IDN;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks up a domain's real expiration date from the registry via WHOIS (port 43).
 * Primary target is .kr (KISA / whois.kr). Returns null gracefully when the registry
 * is unreachable or the date can't be parsed.
 */
@Component
public class DomainExpiryLookup {

    private static final Logger log = LoggerFactory.getLogger(DomainExpiryLookup.class);

    private static final Pattern EXPIRY_LINE =
        Pattern.compile("(?i)(expiration date|registry expiry date|expiry date|사용\\s*종료일)");
    private static final Pattern DATE_PATTERN =
        Pattern.compile("(\\d{4})[.\\-/년\\s]+(\\d{1,2})[.\\-/월\\s]+(\\d{1,2})");

    private static final int TIMEOUT_MS = 10_000;

    /**
     * Look up the expiration date for the given domain name. Returns null if unavailable.
     */
    public Instant lookupExpiration(String domainName) {
        if (domainName == null || domainName.isBlank()) {
            return null;
        }

        String punycode;
        try {
            punycode = IDN.toASCII(domainName.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        String tld = punycode.substring(punycode.lastIndexOf('.') + 1);
        String server = whoisServerFor(tld);
        if (server == null) {
            log.warn("No WHOIS server configured for TLD .{} ({})", tld, domainName);
            return null;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server, 43), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8);
            out.print(punycode + "\r\n");
            out.flush();

            StringBuilder sb = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }

            LocalDate expiry = parseExpiry(sb.toString());
            return expiry == null ? null : expiry.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            log.warn("WHOIS expiration lookup failed for {}: {}", domainName, e.getMessage());
            return null;
        }
    }

    private String whoisServerFor(String tld) {
        return switch (tld) {
            case "kr" -> "whois.kr";
            case "com", "net" -> "whois.verisign-grs.com";
            case "org" -> "whois.pir.org";
            default -> null;
        };
    }

    private LocalDate parseExpiry(String text) {
        for (String line : text.split("\\R")) {
            if (EXPIRY_LINE.matcher(line).find()) {
                Matcher m = DATE_PATTERN.matcher(line);
                if (m.find()) {
                    try {
                        int year = Integer.parseInt(m.group(1));
                        int month = Integer.parseInt(m.group(2));
                        int day = Integer.parseInt(m.group(3));
                        return LocalDate.of(year, month, day);
                    } catch (Exception ignore) {
                        // keep scanning other lines
                    }
                }
            }
        }
        return null;
    }
}
