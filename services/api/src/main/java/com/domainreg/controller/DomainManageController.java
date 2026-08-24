package com.domainreg.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import com.domainreg.core.entity.Domain;
import com.domainreg.core.vo.Nameserver;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.DomainManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/domains/{id}")
public class DomainManageController {

    private final DomainManagementService managementService;

    public DomainManageController(DomainManagementService managementService) {
        this.managementService = managementService;
    }

    // -- Delete --

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteDomain(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        managementService.deleteDomain(principal.getUserId(), id);

        return ResponseEntity.ok(Map.of(
            "message", "도메인이 삭제되었습니다."
        ));
    }

    // -- Renew --

    @PostMapping("/renew")
    public ResponseEntity<Domain> renewDomain(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(managementService.renewDomain(principal.getUserId(), id));
    }

    // -- Nameservers --

    @GetMapping("/nameservers")
    public ResponseEntity<List<Nameserver>> getNameservers(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getNameservers(id));
    }

    @PutMapping("/nameservers")
    public ResponseEntity<Map<String, Object>> updateNameservers(
            @PathVariable Long id,
            @Valid @RequestBody NameserverRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<Nameserver> ns = request.nameservers().stream()
            .map(n -> new Nameserver(n.host(), n.ip()))
            .toList();

        managementService.updateNameservers(principal.getUserId(), id, ns);

        return ResponseEntity.ok(Map.of(
            "status", "NS_UPDATING",
            "message", "네임서버 변경이 요청되었습니다. 수 분 내로 반영됩니다."
        ));
    }

    // -- Privacy --

    @GetMapping("/privacy")
    public ResponseEntity<Map<String, Object>> getPrivacy(@PathVariable Long id) {
        boolean enabled = managementService.getPrivacyStatus(id);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @PutMapping("/privacy")
    public ResponseEntity<Map<String, Object>> togglePrivacy(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        boolean enabled = body.getOrDefault("enabled", true);
        managementService.togglePrivacy(principal.getUserId(), id, enabled);

        return ResponseEntity.ok(Map.of(
            "privacyEnabled", enabled,
            "message", enabled ? "Privacy 보호가 활성화되었습니다." : "Privacy 보호가 해제되었습니다."
        ));
    }

    public record NameserverRequest(
        @NotEmpty List<NameserverEntry> nameservers
    ) {}

    public record NameserverEntry(String host, String ip) {}
}
