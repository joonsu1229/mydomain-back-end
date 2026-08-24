package com.domainreg.service;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.PrivacyProfile;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.vo.Nameserver;
import com.domainreg.persistence.mapper.NameserverMapper;
import com.domainreg.persistence.mapper.PrivacyProfileMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.IDN;
import java.time.LocalDate;
import java.util.*;

@Service
public class WhoisService {

    private final DomainRepository domainRepository;
    private final NameserverMapper nameserverMapper;
    private final PrivacyProfileMapper privacyMapper;

    public WhoisService(DomainRepository domainRepository,
                        NameserverMapper nameserverMapper,
                        PrivacyProfileMapper privacyMapper) {
        this.domainRepository = domainRepository;
        this.nameserverMapper = nameserverMapper;
        this.privacyMapper = privacyMapper;
    }

    @Cacheable(value = "whois", key = "#query.trim().toLowerCase()")
    public Map<String, Object> lookup(String query) {
        String punycode;
        try {
            punycode = IDN.toASCII(query.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            return Map.of("error", "INVALID_DOMAIN", "message", "유효하지 않은 도메인입니다.");
        }

        Optional<Domain> domainOpt = domainRepository.findByPunycode(punycode);
        if (domainOpt.isEmpty()) {
            return Map.of(
                "domain", query,
                "available", true,
                "message", "등록 가능한 도메인입니다."
            );
        }

        Domain domain = domainOpt.get();
        List<Nameserver> nameservers = nameserverMapper.findByDomainId(domain.getId());
        boolean isPrivacy = domain.isPrivacyEnabled();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("domain", domain.getNameUnicode());
        result.put("punycode", domain.getNamePunycode());
        result.put("tld", domain.getTld());
        result.put("status", domain.getStatus().name());
        result.put("registeredAt", domain.getCreatedAt());
        result.put("expiresAt", domain.getExpiresAt());
        result.put("nameservers", nameservers.stream().map(ns -> Map.of("host", ns.host(), "ip", ns.ip())).toList());

        // Privacy masking
        if (isPrivacy) {
            result.put("privacy", true);
            result.put("registrant", "REDACTED FOR PRIVACY");
            result.put("registrantEmail", "REDACTED FOR PRIVACY");
            result.put("registrantPhone", "REDACTED FOR PRIVACY");

            // Show proxy contact if available
            privacyMapper.findByDomainId(domain.getId()).ifPresent(pp -> {
                result.put("privacyProxyEmail", pp.getProxyEmail());
            });
        } else {
            result.put("privacy", false);
            result.put("registrant", "등록인 정보는 로그인 후 확인 가능합니다.");
        }

        return result;
    }
}
