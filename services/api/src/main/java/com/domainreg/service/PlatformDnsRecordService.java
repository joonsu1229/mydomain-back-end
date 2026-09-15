package com.domainreg.service;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.registrar.powerdns.PowerDnsApiClient;
import com.domainreg.registrar.powerdns.PowerDnsProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 루트(플랫폼) 도메인의 TXT 레코드를 PowerDNS에서 직접 관리한다.
 * (서브도메인의 dns_records와 달리, 루트 도메인 레코드는 PowerDNS 존에 바로 반영한다)
 */
@Service
public class PlatformDnsRecordService {

    private final PowerDnsApiClient api;
    private final DomainRepository domainRepository;

    public PlatformDnsRecordService(PowerDnsProperties properties, DomainRepository domainRepository) {
        this.api = new PowerDnsApiClient(properties.getBaseUrl(), properties.getApiKey());
        this.domainRepository = domainRepository;
    }

    /**
     * 존(예: rog.kr)의 TXT 레코드 중 루트 도메인에 등록된 것만 조회한다.
     * 사용자가 발급한 서브도메인 하위(예: _acme-challenge.blog.rog.kr)의 TXT는 제외한다.
     */
    public List<TxtRecord> listTxtRecords(Long platformDomainId, String zoneName) {
        List<TxtRecord> result = new ArrayList<>();
        PowerDnsApiClient.Zone zone = api.getZone(zoneName);
        if (zone.rrsets() == null) {
            return result;
        }

        // 사용자 서브도메인 목록 — 이 하위에 등록된 TXT는 루트 도메인 레코드가 아니다.
        Set<String> subdomains = new HashSet<>();
        for (Domain d : domainRepository.findByPlatformDomainId(platformDomainId)) {
            String sub = trimDot(d.getNamePunycode()).toLowerCase(Locale.ROOT);
            if (!sub.isEmpty()) {
                subdomains.add(sub);
            }
        }

        for (PowerDnsApiClient.Rrset rr : zone.rrsets()) {
            if (!"TXT".equalsIgnoreCase(rr.type()) || rr.records() == null) {
                continue;
            }
            String name = trimDot(rr.name()).toLowerCase(Locale.ROOT);
            if (isUnderSubdomain(name, subdomains)) {
                continue; // 서브도메인 TXT 제외
            }
            for (PowerDnsApiClient.Record rec : rr.records()) {
                result.add(new TxtRecord(trimDot(rr.name()), stripQuotes(rec.content())));
            }
        }
        return result;
    }

    /** name이 등록된 서브도메인과 같거나 그 하위인지 검사한다. */
    private boolean isUnderSubdomain(String name, Set<String> subdomains) {
        for (String sub : subdomains) {
            if (name.equals(sub) || name.endsWith("." + sub)) {
                return true;
            }
        }
        return false;
    }

    public void addTxtRecord(String zoneName, String name, String content) {
        String fqdn = name.endsWith(".") ? name : name + ".";
        // PowerDNS는 TXT content에 따옴표를 요구한다
        String quoted = content.startsWith("\"") ? content : "\"" + content + "\"";
        api.patchRrsets(zoneName, List.of(
            new PowerDnsApiClient.RrsetChange(fqdn, "TXT", 300, "REPLACE",
                List.of(new PowerDnsApiClient.Record(quoted, false)))));
    }

    public void deleteTxtRecord(String zoneName, String name) {
        String fqdn = name.endsWith(".") ? name : name + ".";
        api.patchRrsets(zoneName, List.of(
            new PowerDnsApiClient.RrsetChange(fqdn, "TXT", 300, "DELETE", List.of())));
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

    /** TXT content에서 감싼 따옴표를 제거한다 (화면 표시용). */
    private String stripQuotes(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    public record TxtRecord(String name, String content) {}
}
