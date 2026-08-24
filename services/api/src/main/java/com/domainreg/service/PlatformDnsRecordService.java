package com.domainreg.service;

import com.domainreg.registrar.powerdns.PowerDnsApiClient;
import com.domainreg.registrar.powerdns.PowerDnsProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 루트(플랫폼) 도메인의 TXT 레코드를 PowerDNS에서 직접 관리한다.
 * (서브도메인의 dns_records와 달리, 루트 도메인 레코드는 PowerDNS 존에 바로 반영한다)
 */
@Service
public class PlatformDnsRecordService {

    private final PowerDnsApiClient api;

    public PlatformDnsRecordService(PowerDnsProperties properties) {
        this.api = new PowerDnsApiClient(properties.getBaseUrl(), properties.getApiKey());
    }

    /** 존(예: rog.kr)의 TXT 레코드 전체를 조회한다. */
    public List<TxtRecord> listTxtRecords(String zoneName) {
        List<TxtRecord> result = new ArrayList<>();
        PowerDnsApiClient.Zone zone = api.getZone(zoneName);
        if (zone.rrsets() == null) {
            return result;
        }
        for (PowerDnsApiClient.Rrset rr : zone.rrsets()) {
            if (!"TXT".equalsIgnoreCase(rr.type()) || rr.records() == null) {
                continue;
            }
            for (PowerDnsApiClient.Record rec : rr.records()) {
                result.add(new TxtRecord(trimDot(rr.name()), stripQuotes(rec.content())));
            }
        }
        return result;
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
