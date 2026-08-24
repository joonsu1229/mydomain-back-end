package com.domainreg.controller;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.DomainSearchService;
import com.domainreg.core.vo.SearchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DomainSearchController {

    private final DomainSearchService searchService;
    private final DomainRepository domainRepository;

    public DomainSearchController(DomainSearchService searchService, DomainRepository domainRepository) {
        this.searchService = searchService;
        this.domainRepository = domainRepository;
    }

    @GetMapping("/domains/search")
    public ResponseEntity<SearchResult> search(
            @RequestParam("q") String query,
            @RequestParam(value = "platformDomainId", required = false) Long platformDomainId) {
        SearchResult result = searchService.search(query, platformDomainId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/domains")
    public ResponseEntity<List<Domain>> myDomains(@AuthenticationPrincipal UserPrincipal principal) {
        List<Domain> domains = domainRepository.findByUserId(principal.getUserId());
        return ResponseEntity.ok(domains);
    }

    @GetMapping("/domains/{id}")
    public ResponseEntity<Domain> getDomain(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        Domain domain = domainRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다."));

        // Ownership check
        if (!domain.getUserId().equals(principal.getUserId())
            && !"ADMIN".equals(principal.getRole())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(domain);
    }
}
