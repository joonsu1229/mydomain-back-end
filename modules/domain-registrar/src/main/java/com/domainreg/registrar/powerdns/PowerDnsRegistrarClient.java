package com.domainreg.registrar.powerdns;

import com.domainreg.core.entity.DnsRecord;
import com.domainreg.core.port.RegistrarClient;
import com.domainreg.core.vo.AvailabilityResult;
import com.domainreg.core.vo.DomainInfo;
import com.domainreg.core.vo.Nameserver;
import com.domainreg.core.vo.RegisterCommand;
import com.domainreg.core.vo.RegisterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RegistrarClient} backed by a self-hosted PowerDNS authoritative server.
 *
 * <p>Enabled with {@code app.registrar.mode=powerdns}. Unlike the Cloudflare
 * client, records are served straight from the PowerDNS zone (a single parent zone such
 * as {@code rog.kr}), so there is no per-zone DNS record limit — the platform scales to
 * as many subdomains as the PowerDNS database can hold.
 *
 * <p>Registrar-side operations (external domain registration, WHOIS privacy) are out of
 * scope for the subdomain product and return "not supported", mirroring the cloudflare client.
 */
@Component
@ConditionalOnProperty(name = "app.registrar.mode", havingValue = "powerdns")
public class PowerDnsRegistrarClient implements RegistrarClient {

    private static final Logger log = LoggerFactory.getLogger(PowerDnsRegistrarClient.class);

    private final PowerDnsApiClient api;

    public PowerDnsRegistrarClient(PowerDnsProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                "app.powerdns.api-key must be set when app.registrar.mode=powerdns");
        }
        this.api = new PowerDnsApiClient(properties.getBaseUrl(), properties.getApiKey());
    }

    @Override
    public void syncDnsRecords(String zoneName, String domainName, List<DnsRecord> records) {
        String zone = resolveZone(zoneName, domainName);
        List<PowerDnsApiClient.Rrset> existing = api.getZone(zone).rrsets();
        if (existing == null) {
            existing = List.of();
        }

        // Desired records, keyed by TYPE|fqdn.
        Map<String, DnsRecord> desired = new HashMap<>();
        for (DnsRecord r : records) {
            String type = r.getRecordType() == null ? "" : r.getRecordType().toUpperCase();
            if (!isSyncable(type)) {
                log.warn("Skipping unsupported DNS record type {} for domain {}", type, domainName);
                continue;
            }
            desired.put(key(type, toFqdn(domainName, r.getName())), r);
        }

        List<PowerDnsApiClient.RrsetChange> changes = new ArrayList<>();

        // Delete stale rrsets that belong to this domain but are no longer desired.
        for (PowerDnsApiClient.Rrset rr : existing) {
            String rrName = trimDot(rr.name());
            if (!inDomain(rrName, domainName)) {
                continue;
            }
            if (!desired.containsKey(key(rr.type(), rrName))) {
                changes.add(new PowerDnsApiClient.RrsetChange(
                    rr.name(), rr.type(), rr.ttl(), "DELETE", List.of()));
            }
        }

        // Create / replace desired records.
        for (Map.Entry<String, DnsRecord> e : desired.entrySet()) {
            DnsRecord r = e.getValue();
            String type = r.getRecordType().toUpperCase();
            String fqdn = toFqdn(domainName, r.getName());
            changes.add(new PowerDnsApiClient.RrsetChange(
                fqdn + ".", type, normalizeTtl(r.getTtl()), "REPLACE",
                List.of(new PowerDnsApiClient.Record(content(type, r), false))));
        }

        if (!changes.isEmpty()) {
            api.patchRrsets(zone, changes);
            log.info("PowerDNS: synced {} rrset changes for {} in zone {}",
                changes.size(), domainName, zone);
        }
    }

    @Override
    public void updateNameservers(String zoneName, String domainName, List<Nameserver> ns) {
        String zone = resolveZone(zoneName, domainName);
        List<PowerDnsApiClient.Rrset> existing = api.getZone(zone).rrsets();
        if (existing == null) {
            existing = List.of();
        }

        List<PowerDnsApiClient.RrsetChange> changes = new ArrayList<>();
        for (PowerDnsApiClient.Rrset rr : existing) {
            if ("NS".equalsIgnoreCase(rr.type()) && trimDot(rr.name()).equals(domainName)) {
                changes.add(new PowerDnsApiClient.RrsetChange(
                    rr.name(), rr.type(), rr.ttl(), "DELETE", List.of()));
            }
        }

        List<PowerDnsApiClient.Record> recs = new ArrayList<>();
        for (Nameserver n : ns) {
            if (n.host() != null && !n.host().isBlank()) {
                String host = n.host().trim();
                recs.add(new PowerDnsApiClient.Record(host.endsWith(".") ? host : host + ".", false));
            }
        }
        if (!recs.isEmpty()) {
            changes.add(new PowerDnsApiClient.RrsetChange(
                domainName + ".", "NS", 3600, "REPLACE", recs));
        }

        if (!changes.isEmpty()) {
            api.patchRrsets(zone, changes);
            log.info("PowerDNS: updated NS for {} in zone {}", domainName, zone);
        }
    }

    // -- Registrar-side operations (out of scope for the subdomain product) --

    @Override
    public AvailabilityResult checkAvailability(String punycodeName) {
        return AvailabilityResult.unavailable("외부 도메인 등록은 powerdns 모드에서 지원되지 않습니다.");
    }

    @Override
    public RegisterResult register(RegisterCommand cmd) {
        return RegisterResult.failure("외부 도메인 등록은 powerdns 모드에서 지원되지 않습니다.");
    }

    @Override
    public void setPrivacy(String registrarRef, boolean enabled) {
        // Subdomains have no registrar-side WHOIS record; tracked in the platform DB only.
        log.info("PowerDNS: setPrivacy({}, {}) is a no-op", registrarRef, enabled);
    }

    @Override
    public DomainInfo getDomain(String registrarRef) {
        return DomainInfo.builder()
            .registrarRef(registrarRef)
            .expiresAt(LocalDate.now().plusYears(1))
            .build();
    }

    // -- helpers --

    private boolean isSyncable(String type) {
        return switch (type) {
            case "A", "AAAA", "CNAME", "MX", "TXT", "NS" -> true;
            default -> false; // SRV etc. need PowerDNS's rich content format — not supported yet
        };
    }

    private String resolveZone(String zoneName, String domainName) {
        if (zoneName == null || zoneName.isBlank()) {
            throw new PowerDnsApiException(
                "No PowerDNS zone resolved for domain " + domainName
                    + "; ensure the platform domain is registered and active");
        }
        return zoneName;
    }

    /**
     * Map a record's {@code name} (alias or {@code @}) to a fully-qualified name under
     * the managed domain. {@code @} maps to the domain apex; {@code www} maps to
     * {@code www.{domain}}.
     */
    private String toFqdn(String base, String name) {
        if (name == null || name.isBlank() || "@".equals(name)) {
            return base;
        }
        if (name.endsWith("." + base)) {
            return name;
        }
        return name + "." + base;
    }

    private String content(String type, DnsRecord r) {
        String content = r.getContent() == null ? "" : r.getContent().trim();
        if ("MX".equals(type)) {
            int prio = r.getPriority() != null ? r.getPriority() : 10;
            return prio + " " + fqdn(content);
        }
        if ("CNAME".equals(type) || "NS".equals(type)) {
            return fqdn(content);
        }
        if ("TXT".equals(type)) {
            // PowerDNS requires TXT content to be quoted ("value"), same as PlatformDnsRecordService.
            return content.startsWith("\"") ? content : "\"" + content + "\"";
        }
        return content;
    }

    /** Make a name absolute (trailing dot) — PowerDNS requires FQDN content for CNAME/NS/MX. */
    private String fqdn(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.endsWith(".") ? s : s + ".";
    }

    private int normalizeTtl(int ttl) {
        if (ttl <= 0) {
            return 3600;
        }
        return Math.max(60, Math.min(86400, ttl));
    }

    private String key(String type, String fqdn) {
        return (type == null ? "" : type.toUpperCase()) + "|" + trimDot(fqdn).toLowerCase();
    }

    private boolean inDomain(String rrName, String domainName) {
        String a = trimDot(rrName).toLowerCase();
        String b = trimDot(domainName).toLowerCase();
        return a.equals(b) || a.endsWith("." + b);
    }

    private String trimDot(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim();
        while (v.endsWith(".")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
