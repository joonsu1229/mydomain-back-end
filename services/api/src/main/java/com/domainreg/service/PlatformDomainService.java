package com.domainreg.service;

import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.port.PlatformDomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.IDN;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PlatformDomainService {

    private static final String DNS_PREFIX = "_domainon.";
    private static final String TOKEN_PREFIX = "domainon-verify=";

    private final PlatformDomainRepository repository;
    private final DnsLookup dnsLookup;
    private final DomainExpiryLookup expiryLookup;

    public PlatformDomainService(PlatformDomainRepository repository, DnsLookup dnsLookup,
                                 DomainExpiryLookup expiryLookup) {
        this.repository = repository;
        this.dnsLookup = dnsLookup;
        this.expiryLookup = expiryLookup;
    }

    public List<PlatformDomain> getAllPlatformDomains() {
        return repository.findAll();
    }

    public List<PlatformDomain> getActivePlatformDomains() {
        return repository.findAllActive();
    }

    public PlatformDomain getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("플랫폼 도메인을 찾을 수 없습니다."));
    }

    @Transactional
    public PlatformDomain addPlatformDomain(String domainName, String displayName, String description,
                                            Instant expiresAt) {
        String trimmed = domainName.trim().toLowerCase();

        // Validate and convert to punycode
        String punycode;
        try {
            punycode = IDN.toASCII(trimmed);
        } catch (IllegalArgumentException e) {
            throw new PlatformDomainException("INVALID_DOMAIN", "유효하지 않은 도메인 이름입니다.");
        }

        // Check uniqueness
        if (repository.findByPunycode(punycode).isPresent()) {
            throw new PlatformDomainException("DUPLICATE", "이미 등록된 플랫폼 도메인입니다.");
        }

        // Auto-lookup the real expiration date when not provided manually
        if (expiresAt == null) {
            expiresAt = expiryLookup.lookupExpiration(trimmed);
        }

        // Generate verification token and set status PENDING
        String token = UUID.randomUUID().toString().replace("-", "");

        PlatformDomain pd = new PlatformDomain();
        pd.setNameUnicode(trimmed);
        pd.setNamePunycode(punycode);
        pd.setDisplayName(displayName != null ? displayName : trimmed);
        pd.setDescription(description);
        pd.setStatus("PENDING");
        pd.setVerificationToken(token);
        pd.setExpiresAt(expiresAt);
        pd.setActive(false); // not usable until TXT ownership is verified
        repository.save(pd);
        return pd;
    }

    /**
     * Look up a domain's real expiration date from the registry (WHOIS).
     */
    public Instant lookupExpirationDate(String domain) {
        return expiryLookup.lookupExpiration(domain);
    }

    /**
     * Verify domain ownership via DNS TXT record.
     * Looks up _domainon.{domain} and checks for TXT record matching the verification token.
     */
    @Transactional
    public PlatformDomain verifyDomain(Long id) {
        PlatformDomain pd = getById(id);

        if ("ACTIVE".equals(pd.getStatus())) {
            throw new PlatformDomainException("ALREADY_VERIFIED", "이미 인증된 도메인입니다.");
        }

        if (pd.getVerificationToken() == null) {
            throw new PlatformDomainException("NO_TOKEN", "인증 토큰이 없습니다. 다시 등록해주세요.");
        }

        // DNS TXT lookup: _domainon.example.kr
        String verificationHost = DNS_PREFIX + pd.getNameUnicode();
        List<String> txtRecords = dnsLookup.lookupTxt(verificationHost);

        String expectedValue = TOKEN_PREFIX + pd.getVerificationToken();
        boolean found = txtRecords.stream().anyMatch(r -> r.contains(expectedValue));

        if (!found) {
            throw new PlatformDomainException("VERIFY_FAILED",
                "DNS TXT 레코드를 찾을 수 없습니다. " + verificationHost +
                " 에 TXT 레코드로 '" + expectedValue + "' 를 추가했는지 확인해주세요. " +
                "(DNS 전파에 최대 5분 소요될 수 있습니다)");
        }

        pd.setStatus("ACTIVE");
        pd.setVerifiedAt(Instant.now());
        pd.setActive(true);
        repository.update(pd);
        return pd;
    }

    /**
     * Get DNS verification instructions for a domain.
     */
    public String getVerificationInstructions(Long id) {
        PlatformDomain pd = getById(id);
        String host = DNS_PREFIX + pd.getNameUnicode();
        String value = TOKEN_PREFIX + pd.getVerificationToken();
        return "DNS 관리자에서 아래 TXT 레코드를 추가하세요:\n" +
               "  호스트: " + host + "\n" +
               "  값: " + value + "\n" +
               "  TTL: 300 (5분)\n\n" +
               "추가 후 '인증 확인'을 클릭하면 소유권이 확인됩니다.";
    }

    @Transactional
    public PlatformDomain updatePlatformDomain(Long id, String displayName, String description,
                                               Instant expiresAt, Boolean isActive) {
        PlatformDomain pd = getById(id);
        if (Boolean.TRUE.equals(isActive) && pd.getVerifiedAt() == null) {
            throw new PlatformDomainException("NOT_VERIFIED",
                "TXT 인증이 완료되어야 활성화할 수 있습니다.");
        }
        if (displayName != null) {
            pd.setDisplayName(displayName);
        }
        if (description != null) {
            pd.setDescription(description);
        }
        if (expiresAt != null) {
            pd.setExpiresAt(expiresAt);
        }
        if (isActive != null) {
            pd.setActive(isActive);
            if (!isActive) {
                pd.setStatus("INACTIVE");
            }
        }
        repository.update(pd);
        return pd;
    }

    @Transactional
    public PlatformDomain activatePlatformDomain(Long id) {
        PlatformDomain pd = getById(id);
        if (pd.getVerifiedAt() == null) {
            throw new PlatformDomainException("NOT_VERIFIED",
                "TXT 인증이 완료되어야 활성화할 수 있습니다.");
        }
        // Check expiry
        if (pd.getExpiresAt() != null && pd.getExpiresAt().isBefore(Instant.now())) {
            throw new PlatformDomainException("DOMAIN_EXPIRED",
                "만료된 도메인은 활성화할 수 없습니다. 만료일을 갱신해주세요.");
        }
        pd.setActive(true);
        pd.setStatus("ACTIVE");
        repository.update(pd);
        return pd;
    }

    @Transactional
    public void deactivatePlatformDomain(Long id) {
        PlatformDomain pd = getById(id);
        pd.setActive(false);
        pd.setStatus("INACTIVE");
        repository.update(pd);
    }

    @Transactional
    public void deletePlatformDomain(Long id) {
        repository.delete(id); // soft-delete (sets is_active=false)
    }

    @Transactional
    public void hardDeletePlatformDomain(Long id) {
        getById(id); // ensure it exists
        // Detach referencing subdomains first, otherwise the FK (domains.platform_domain_id)
        // would block the delete with a constraint violation.
        repository.detachDomains(id);
        repository.hardDelete(id); // permanent delete from DB
    }

    public static class PlatformDomainException extends RuntimeException {
        private final String code;
        public PlatformDomainException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
