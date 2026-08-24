package com.domainreg.controller;

import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.service.PlatformDomainService;
import com.domainreg.service.PlatformDnsRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlatformDomainController {

    private final PlatformDomainService platformDomainService;
    private final PlatformDnsRecordService platformDnsRecordService;

    public PlatformDomainController(PlatformDomainService platformDomainService,
                                    PlatformDnsRecordService platformDnsRecordService) {
        this.platformDomainService = platformDomainService;
        this.platformDnsRecordService = platformDnsRecordService;
    }

    /**
     * Public endpoint — list active platform domains for subdomain creation UI.
     */
    @GetMapping("/platform-domains/active")
    public ResponseEntity<List<PlatformDomain>> getActivePlatformDomains() {
        return ResponseEntity.ok(platformDomainService.getActivePlatformDomains());
    }

    /**
     * Public endpoint — get a specific platform domain.
     */
    @GetMapping("/platform-domains/{id}")
    public ResponseEntity<PlatformDomain> getPlatformDomain(@PathVariable Long id) {
        return ResponseEntity.ok(platformDomainService.getById(id));
    }

    /**
     * Admin endpoint — list all platform domains (including inactive).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/platform-domains")
    public ResponseEntity<List<PlatformDomain>> getAllPlatformDomains() {
        return ResponseEntity.ok(platformDomainService.getAllPlatformDomains());
    }

    /**
     * Admin endpoint — register a new platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/platform-domains")
    public ResponseEntity<PlatformDomain> addPlatformDomain(@RequestBody Map<String, String> body) {
        String domainName = body.get("domainName");
        String displayName = body.getOrDefault("displayName", domainName);
        String description = body.get("description");
        Instant expiresAt = parseDate(body.get("expiresAt"));
        PlatformDomain pd = platformDomainService.addPlatformDomain(domainName, displayName, description, expiresAt);
        return ResponseEntity.ok(pd);
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Admin endpoint — update a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/platform-domains/{id}")
    public ResponseEntity<PlatformDomain> updatePlatformDomain(@PathVariable Long id,
                                                                @RequestBody Map<String, Object> body) {
        String displayName = (String) body.get("displayName");
        String description = (String) body.get("description");
        Instant expiresAt = body.get("expiresAt") != null ? parseDate(body.get("expiresAt").toString()) : null;
        Boolean isActive = body.containsKey("isActive") ? (Boolean) body.get("isActive") : null;
        PlatformDomain pd = platformDomainService.updatePlatformDomain(id, displayName, description, expiresAt, isActive);
        return ResponseEntity.ok(pd);
    }

    /**
     * Admin endpoint — activate a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/platform-domains/{id}/activate")
    public ResponseEntity<PlatformDomain> activateDomain(@PathVariable Long id) {
        return ResponseEntity.ok(platformDomainService.activatePlatformDomain(id));
    }

    /**
     * Admin endpoint — deactivate a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/platform-domains/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateDomain(@PathVariable Long id) {
        platformDomainService.deactivatePlatformDomain(id);
        return ResponseEntity.ok(Map.of("message", "비활성화 완료"));
    }

    /**
     * Admin endpoint — verify domain ownership via DNS TXT record.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/platform-domains/{id}/verify")
    public ResponseEntity<PlatformDomain> verifyDomain(@PathVariable Long id) {
        return ResponseEntity.ok(platformDomainService.verifyDomain(id));
    }

    /**
     * Admin endpoint — get DNS verification instructions.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/platform-domains/{id}/verify-instructions")
    public ResponseEntity<Map<String, String>> getVerificationInstructions(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("instructions", platformDomainService.getVerificationInstructions(id)));
    }

    /**
     * Admin endpoint — look up a domain's real expiration date from the registry (WHOIS).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/platform-domains/lookup-expiration")
    public ResponseEntity<Map<String, Object>> lookupExpiration(@RequestParam("domain") String domain) {
        Instant expiresAt = platformDomainService.lookupExpirationDate(domain);
        Map<String, Object> body = new HashMap<>();
        body.put("expiresAt", expiresAt);
        return ResponseEntity.ok(body);
    }

    /**
     * Admin endpoint — list TXT records of a platform domain (root domain).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/platform-domains/{id}/txt")
    public ResponseEntity<List<PlatformDnsRecordService.TxtRecord>> listTxtRecords(@PathVariable Long id) {
        String zoneName = platformDomainService.getById(id).getNameUnicode();
        return ResponseEntity.ok(platformDnsRecordService.listTxtRecords(zoneName));
    }

    /**
     * Admin endpoint — add a TXT record to a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/platform-domains/{id}/txt")
    public ResponseEntity<Map<String, String>> addTxtRecord(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body) {
        String zoneName = platformDomainService.getById(id).getNameUnicode();
        platformDnsRecordService.addTxtRecord(zoneName, body.get("name"), body.get("content"));
        return ResponseEntity.ok(Map.of("message", "TXT 레코드가 추가되었습니다."));
    }

    /**
     * Admin endpoint — delete a TXT record from a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/platform-domains/{id}/txt")
    public ResponseEntity<Map<String, String>> deleteTxtRecord(@PathVariable Long id,
                                                               @RequestBody Map<String, String> body) {
        String zoneName = platformDomainService.getById(id).getNameUnicode();
        platformDnsRecordService.deleteTxtRecord(zoneName, body.get("name"));
        return ResponseEntity.ok(Map.of("message", "TXT 레코드가 삭제되었습니다."));
    }

    /**
     * Admin endpoint — permanently delete a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/platform-domains/{id}/permanent")
    public ResponseEntity<Map<String, String>> permanentlyDeletePlatformDomain(@PathVariable Long id) {
        platformDomainService.hardDeletePlatformDomain(id);
        return ResponseEntity.ok(Map.of("message", "플랫폼 도메인이 영구 삭제되었습니다."));
    }

    /**
     * Admin endpoint — deactivate a platform domain.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/platform-domains/{id}")
    public ResponseEntity<Map<String, String>> deletePlatformDomain(@PathVariable Long id) {
        platformDomainService.deletePlatformDomain(id);
        return ResponseEntity.ok(Map.of("message", "플랫폼 도메인이 비활성화되었습니다."));
    }
}
