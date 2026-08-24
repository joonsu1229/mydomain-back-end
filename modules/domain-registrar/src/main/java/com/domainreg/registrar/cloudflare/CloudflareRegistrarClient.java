package com.domainreg.registrar.cloudflare;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Real {@link RegistrarClient} backed by the Cloudflare DNS API.
 *
 * <p>Enabled with {@code app.registrar.mode=cloudflare}. The primary operation is
 * {@link #syncDnsRecords}, which reconciles a domain's DNS records in the Cloudflare
 * zone: create missing, update changed, and delete records that were removed in the DB.
 * Managed records are tagged with a {@code comment} so only records this platform owns
 * are ever touched.
 *
 * <p>Registrar-side operations (domain registration, WHOIS privacy) are out of scope —
 * this platform's product is subdomains under a platform domain, which never hit those
 * paths.
 */
@Component
@ConditionalOnProperty(name = "app.registrar.mode", havingValue = "cloudflare")
public class CloudflareRegistrarClient implements RegistrarClient {

    private static final Logger log = LoggerFactory.getLogger(CloudflareRegistrarClient.class);
    private static final String COMMENT_PREFIX = "domainon:";

    private final CloudflareApiClient api;
    private final CloudflareProperties properties;

    public CloudflareRegistrarClient(CloudflareProperties properties) {
        this.properties = properties;
        if (properties.getApiToken() == null || properties.getApiToken().isBlank()) {
            throw new IllegalStateException(
                "app.cloudflare.api-token must be set when app.registrar.mode=cloudflare");
        }
        this.api = new CloudflareApiClient(properties.getApiToken(), properties.getBaseUrl());
    }

    @Override
    public void syncDnsRecords(String zoneName, String domainName, List<DnsRecord> records) {
        String zone = resolveZone(zoneName, domainName);
        String zoneId = api.resolveZoneId(zone);
        String tag = COMMENT_PREFIX + domainName;

        // Existing Cloudflare records that this platform owns for this domain.
        List<CloudflareApiClient.DnsRecordDto> existing = api.listDnsRecords(zoneId).stream()
            .filter(r -> tag.equals(r.comment()))
            .toList();

        // Desired records (DB state), keyed by type|fqdn.
        Map<String, DnsRecord> desired = new HashMap<>();
        for (DnsRecord r : records) {
            if (isSyncable(r.getRecordType())) {
                desired.put(key(r.getRecordType(), toFqdn(domainName, r.getName())), r);
            } else {
                log.warn("Skipping unsupported DNS record type {} for domain {}",
                    r.getRecordType(), domainName);
            }
        }

        // Create / update.
        for (Map.Entry<String, DnsRecord> e : desired.entrySet()) {
            CloudflareApiClient.DnsRecordDto match = existing.stream()
                .filter(x -> key(x.type(), x.name()).equals(e.getKey()))
                .findFirst()
                .orElse(null);
            CloudflareApiClient.DnsRecordRequest req = toRequest(domainName, e.getValue(), tag);
            if (match == null) {
                api.createDnsRecord(zoneId, req);
                log.info("Cloudflare: created {} {} -> {}", req.type(), req.name(), req.content());
            } else if (!sameContent(match, req)) {
                api.updateDnsRecord(zoneId, match.id(), req);
                log.info("Cloudflare: updated {} {}", req.type(), req.name());
            }
        }

        // Delete stale (owned by this domain but no longer in DB).
        for (CloudflareApiClient.DnsRecordDto x : existing) {
            if (!desired.containsKey(key(x.type(), x.name()))) {
                api.deleteDnsRecord(zoneId, x.id());
                log.info("Cloudflare: deleted {} {}", x.type(), x.name());
            }
        }
    }

    // -- Registrar-side operations (out of scope for this platform) --

    @Override
    public AvailabilityResult checkAvailability(String punycodeName) {
        return AvailabilityResult.unavailable("외부 도메인 등록은 cloudflare 모드에서 지원되지 않습니다.");
    }

    @Override
    public RegisterResult register(RegisterCommand cmd) {
        // Subdomains under a platform domain skip registration upstream; external domain
        // registration via the Cloudflare Registrar API is not implemented.
        return RegisterResult.failure("External domain registration via Cloudflare Registrar is not implemented yet");
    }

    @Override
    public void updateNameservers(String zoneName, String domainName, List<Nameserver> ns) {
        String zone = resolveZone(zoneName, domainName);
        String zoneId = api.resolveZoneId(zone);
        String tag = COMMENT_PREFIX + domainName;

        // Existing records this platform owns for this domain.
        List<CloudflareApiClient.DnsRecordDto> existing = api.listDnsRecords(zoneId).stream()
            .filter(r -> tag.equals(r.comment()))
            .toList();

        // Desired NS delegation records, keyed by normalized host.
        Map<String, Nameserver> desired = new HashMap<>();
        for (Nameserver n : ns) {
            if (n.host() != null && !n.host().isBlank()) {
                desired.put(norm(n.host()), n);
            }
        }

        // Create missing NS records (name = the domain, content = nameserver host).
        for (Map.Entry<String, Nameserver> e : desired.entrySet()) {
            String host = e.getValue().host().trim();
            boolean present = existing.stream()
                .anyMatch(x -> "NS".equalsIgnoreCase(x.type()) && norm(x.content()).equals(e.getKey()));
            if (!present) {
                CloudflareApiClient.DnsRecordRequest req = new CloudflareApiClient.DnsRecordRequest(
                    "NS", domainName, host, 3600, null, false, tag);
                api.createDnsRecord(zoneId, req);
                log.info("Cloudflare: created NS {} -> {}", domainName, host);
            }
        }

        // Reconcile existing records.
        for (CloudflareApiClient.DnsRecordDto x : existing) {
            if ("NS".equalsIgnoreCase(x.type())) {
                if (!desired.containsKey(norm(x.content()))) {
                    api.deleteDnsRecord(zoneId, x.id());
                    log.info("Cloudflare: deleted NS {} {}", x.name(), x.content());
                }
            } else if (!desired.isEmpty()) {
                // Only when delegating (non-empty NS) do we retire the platform's A/MX/TXT.
                api.deleteDnsRecord(zoneId, x.id());
                log.info("Cloudflare: deleted {} {} (domain delegated to custom nameservers)",
                    x.type(), x.name());
            }
        }
    }

    @Override
    public void setPrivacy(String registrarRef, boolean enabled) {
        throw new CloudflareApiException(
            "setPrivacy is not implemented in cloudflare mode (Cloudflare Registrar API is out of scope)");
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
        return switch (type == null ? "" : type.toUpperCase()) {
            case "A", "AAAA", "CNAME", "MX", "TXT", "NS" -> true;
            default -> false; // SRV etc. require Cloudflare's `data` object — not supported yet
        };
    }

    /**
     * The zone is always derived from the domain's platform domain (DB) via
     * {@code RegistrarJobWorker#resolveZoneName}, never from configuration.
     */
    private String resolveZone(String zoneName, String domainName) {
        if (zoneName == null || zoneName.isBlank()) {
            throw new CloudflareApiException(
                "No Cloudflare zone resolved for domain " + domainName
                    + "; ensure the platform domain is registered and active");
        }
        return zoneName;
    }

    /**
     * Map a record's {@code name} (prefix or {@code @}) to a fully-qualified name,
     * relative to the domain being managed (not the Cloudflare zone). For a subdomain
     * {@code myblog.domon.kr} in zone {@code domon.kr}, {@code @} must become
     * {@code myblog.domon.kr}, not the zone apex.
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

    private String key(String type, String name) {
        return type.toUpperCase() + "|" + norm(name);
    }

    /** Lowercase and strip trailing dots so DB names and Cloudflare names compare equal. */
    private String norm(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim().toLowerCase();
        while (v.endsWith(".")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private CloudflareApiClient.DnsRecordRequest toRequest(String base, DnsRecord r, String tag) {
        String type = r.getRecordType().toUpperCase();
        Integer priority = "MX".equals(type) ? (r.getPriority() != null ? r.getPriority() : 10) : null;
        return new CloudflareApiClient.DnsRecordRequest(
            type,
            toFqdn(base, r.getName()),
            r.getContent(),
            normalizeTtl(r.getTtl()),
            priority,
            false, // DNS-only (no orange-cloud proxy) — TXT/MX/NS cannot be proxied
            tag
        );
    }

    private Integer normalizeTtl(int ttl) {
        if (ttl <= 0) {
            return 3600;
        }
        if (ttl == 1) {
            return 1; // Cloudflare "auto"
        }
        return Math.max(60, Math.min(86400, ttl));
    }

    private boolean sameContent(CloudflareApiClient.DnsRecordDto existing,
                                CloudflareApiClient.DnsRecordRequest desired) {
        return existing.type().equalsIgnoreCase(desired.type())
            && existing.name().equalsIgnoreCase(desired.name())
            && Objects.equals(existing.content(), desired.content())
            && Objects.equals(existing.priority(), desired.priority());
    }
}
