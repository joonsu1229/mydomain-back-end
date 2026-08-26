package com.domainreg.service;

import com.domainreg.core.entity.BlocklistKeyword;
import com.domainreg.exception.SecurityPolicyException;
import com.domainreg.threatintel.SecurityProperties;
import com.domainreg.threatintel.ThreatIntelService;
import org.springframework.stereotype.Service;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

/**
 * 사전 차단·검증 오케스트레이션.
 * - 키워드 필터(도메인 prefix + 레코드 name, contains) — 외부 의존이 없어 항상 fail-closed
 * - TTL/우선순위 검증
 * - A/CNAME 대상 위협정보 평판(위임)
 */
@Service
public class SecurityPolicyService {

    private final BlocklistService blocklistService;
    private final ThreatIntelService threatIntelService;
    private final SecurityProperties props;

    public SecurityPolicyService(BlocklistService blocklistService,
                                 ThreatIntelService threatIntelService,
                                 SecurityProperties props) {
        this.blocklistService = blocklistService;
        this.threatIntelService = threatIntelService;
        this.props = props;
    }

    /** 도메인 prefix(또는 레코드 name)에 대해 활성 블랙리스트 키워드 부분 포함 검사. */
    public void validateDomainName(String name) {
        if (!props.isKeywordFilterEnabled() || name == null || name.isBlank()) {
            return;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        List<BlocklistKeyword> keywords = blocklistService.findEnabled();
        for (BlocklistKeyword k : keywords) {
            String kw = k.getKeyword() == null ? "" : k.getKeyword().toLowerCase(Locale.ROOT);
            if (!kw.isEmpty() && normalized.contains(kw)) {
                throw new SecurityPolicyException("BLOCKED_KEYWORD",
                    "사용할 수 없는 이름입니다. (차단 키워드: \"" + kw + "\")");
            }
        }
    }

    /** DNS 레코드 추가/수정 전 검증(이름·TTL·우선순위·내용 형식·위협정보). */
    public void validateRecord(String type, String name, String content, int ttl, Integer priority) {
        String t = type == null ? "" : type.toUpperCase();
        String c = content == null ? "" : content.trim();

        // 1. 레코드 이름 키워드 검사
        validateDomainName(name);

        // 2. TTL 검증 (0이면 기본값 사용으로 간주)
        if (ttl > 0 && (ttl < 60 || ttl > 86400)) {
            throw new SecurityPolicyException("INVALID_TTL",
                "TTL은 60초 이상 86400초(1일) 이하로 설정해주세요.");
        }

        // 3. 우선순위 검증(MX 필수)
        if ("MX".equals(t) && (priority == null || priority < 0 || priority > 65535)) {
            throw new SecurityPolicyException("INVALID_PRIORITY",
                "MX 레코드는 0~65535 사이의 우선순위가 필요합니다.");
        }

        // 4. 내용 형식 검증 (A/AAAA=IP, CNAME/MX=도메인. CNAME·MX는 IP 금지)
        switch (t) {
            case "A" -> {
                if (!isValidIpv4(c)) {
                    throw new SecurityPolicyException("INVALID_CONTENT",
                        "A 레코드 내용은 유효한 IPv4 주소여야 합니다.");
                }
            }
            case "AAAA" -> {
                if (!isValidIpv6(c)) {
                    throw new SecurityPolicyException("INVALID_CONTENT",
                        "AAAA 레코드 내용은 유효한 IPv6 주소여야 합니다.");
                }
            }
            case "CNAME" -> {
                if (!isValidHostname(c)) {
                    throw new SecurityPolicyException("INVALID_CONTENT",
                        "CNAME 레코드 목적지는 IP가 아닌 유효한 도메인이어야 합니다.");
                }
            }
            case "MX" -> {
                if (!isValidHostname(c)) {
                    throw new SecurityPolicyException("INVALID_CONTENT",
                        "MX 레코드 목적지는 IP가 아닌 유효한 도메인이어야 합니다.");
                }
            }
            default -> {
                // TXT 등은 형식 제약 없음
            }
        }

        // 5. 위협정보 평판
        threatIntelService.checkTarget(t, content);
    }

    /** 엄격한 IPv4 검증(각 옥텟 0~255, 선행 0 허용하지 않음). */
    private static boolean isValidIpv4(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String p : parts) {
            if (p.isEmpty() || p.length() > 3) {
                return false;
            }
            if (p.length() > 1 && p.charAt(0) == '0') {
                return false;
            }
            int v = 0;
            for (char ch : p.toCharArray()) {
                if (ch < '0' || ch > '9') {
                    return false;
                }
                v = v * 10 + (ch - '0');
            }
            if (v > 255) {
                return false;
            }
        }
        return true;
    }

    /** IPv6 검증(콜론 포함 + Inet6Address 파싱, DNS 조회 없음). */
    private static boolean isValidIpv6(String s) {
        if (s == null || !s.contains(":")) {
            return false;
        }
        try {
            return InetAddress.getByName(s) instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

    /** CNAME/MX 목적지 도메인 검증(IP 금지, 유효한 라벨). */
    private static boolean isValidHostname(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String host = s.trim();
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty() || host.length() > 253) {
            return false;
        }
        // IP 주소는 도메인이 아님 (MX 목적지=IP 같은 케이스 차단)
        if (isValidIpv4(host) || isValidIpv6(host)) {
            return false;
        }
        for (String label : host.split("\\.")) {
            if (!isValidLabel(label)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidLabel(String label) {
        if (label.isEmpty() || label.length() > 63) {
            return false;
        }
        if (label.charAt(0) == '-' || label.charAt(label.length() - 1) == '-') {
            return false;
        }
        for (int i = 0; i < label.length(); i++) {
            char ch = label.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9')
                || ch == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
