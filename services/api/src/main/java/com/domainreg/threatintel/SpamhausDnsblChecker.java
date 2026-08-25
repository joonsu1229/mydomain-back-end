package com.domainreg.threatintel;

import com.domainreg.service.DnsLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spamhaus DNSBL 기반 IPv4 평판 검사.
 * 대상 IP를 역순으로 뒤집어 {@code <rev-ip>.<zone>}을 A 질의하고,
 * {@code 127.0.0.x} 형태의 응답이 있으면 listed(차단 대상)로 판단한다.
 * 조회 실패/타임아웃은 fail-open(미등재 처리)한다.
 */
@Component
public class SpamhausDnsblChecker {

    private static final Logger log = LoggerFactory.getLogger(SpamhausDnsblChecker.class);

    private final DnsLookup dnsLookup;
    private final SecurityProperties props;

    public SpamhausDnsblChecker(DnsLookup dnsLookup, SecurityProperties props) {
        this.dnsLookup = dnsLookup;
        this.props = props;
    }

    /** 주어진 IPv4가 DNSBL에 등재되어 있으면 true. IPv4가 아니거나 오류 시 false. */
    public boolean isListed(String ipv4) {
        if (!props.isDnsblEnabled()) {
            return false;
        }
        String reversed = reverseIpv4(ipv4);
        if (reversed == null) {
            return false;
        }
        for (String zone : props.getDnsblZones()) {
            try {
                List<String> answers = dnsLookup.lookupA(reversed + "." + zone);
                for (String a : answers) {
                    if (a != null && a.startsWith("127.")) {
                        log.info("DNSBL {} listed IP {} (answer {})", zone, ipv4, a);
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("DNSBL lookup failed for {} in zone {}: {}", ipv4, zone, e.getMessage());
            }
        }
        return false;
    }

    private String reverseIpv4(String ipv4) {
        if (ipv4 == null) {
            return null;
        }
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        for (String p : parts) {
            try {
                int n = Integer.parseInt(p);
                if (n < 0 || n > 255) {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return parts[3] + "." + parts[2] + "." + parts[1] + "." + parts[0];
    }
}
