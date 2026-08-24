package com.domainreg.service;

import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.port.PlatformDomainRepository;
import com.domainreg.core.vo.SearchResult;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

/**
 * Search for subdomain availability under platform-owned root domains.
 * No external registrar calls — availability is checked against our local DB only.
 */
@Service
public class DomainSearchService {

    private final DomainRepository domainRepository;
    private final PlatformDomainRepository platformDomainRepository;

    public DomainSearchService(DomainRepository domainRepository,
                               PlatformDomainRepository platformDomainRepository) {
        this.domainRepository = domainRepository;
        this.platformDomainRepository = platformDomainRepository;
    }

    // @Cacheable(value = "search", key = "#prefix.trim().toLowerCase() + '|' + (#platformDomainId != null ? #platformDomainId : 'all')")
    public SearchResult search(String prefix, Long platformDomainId) {
        String trimmed = prefix.trim().toLowerCase();

        if (platformDomainId != null) {
            // Search under a specific platform domain
            PlatformDomain pd = platformDomainRepository.findById(platformDomainId)
                .orElse(null);
            if (pd == null) {
                return SearchResult.empty(trimmed);
            }

            String fullName = trimmed + "." + pd.getNameUnicode();
            String punycode;
            try {
                punycode = trimmed + "." + pd.getNamePunycode();
                // Verify the combined name is valid
                IDN.toASCII(fullName);
            } catch (IllegalArgumentException e) {
                return SearchResult.empty(trimmed);
            }

            boolean available = !domainRepository.existsByPunycode(punycode);

            List<SearchResult.TldResult> tlds = new ArrayList<>();
            tlds.add(new SearchResult.TldResult(
                pd.getNameUnicode(), available, 0, null
            ));

            return new SearchResult(fullName, punycode, available, 0, "KRW", tlds);
        }

        // Search across all active platform domains
        List<PlatformDomain> platformDomains = platformDomainRepository.findAllActive();
        List<SearchResult.TldResult> tlds = new ArrayList<>();
        boolean anyAvailable = false;
        String firstQuery = trimmed;

        for (PlatformDomain pd : platformDomains) {
            String fullPunycode = trimmed + "." + pd.getNamePunycode();
            boolean available = !domainRepository.existsByPunycode(fullPunycode);
            if (available) {
                anyAvailable = true;
            }
            tlds.add(new SearchResult.TldResult(
                pd.getNameUnicode(), available, 0,
                available ? null : "이미 등록됨"
            ));
        }

        String punycode = tlds.isEmpty() ? trimmed : trimmed + "." + platformDomains.get(0).getNamePunycode();
        return new SearchResult(trimmed, punycode, anyAvailable, 0, "KRW", tlds);
    }

    /**
     * Backward-compatible search with a full domain name query.
     */
    public SearchResult searchByFullName(String query) {
        String trimmed = query.trim().toLowerCase();
        String punycode;
        try {
            punycode = IDN.toASCII(trimmed);
        } catch (IllegalArgumentException e) {
            return SearchResult.empty(trimmed);
        }

        boolean available = !domainRepository.existsByPunycode(punycode);
        return available ? SearchResult.available(trimmed, punycode, 0)
                         : SearchResult.taken(trimmed, punycode);
    }
}
