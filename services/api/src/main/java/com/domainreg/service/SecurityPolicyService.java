package com.domainreg.service;

import com.domainreg.core.entity.BlocklistKeyword;
import com.domainreg.exception.SecurityPolicyException;
import com.domainreg.threatintel.SecurityProperties;
import com.domainreg.threatintel.ThreatIntelService;
import org.springframework.stereotype.Service;

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

    /** DNS 레코드 추가/수정 전 검증(이름·TTL·우선순위·위협정보). */
    public void validateRecord(String type, String name, String content, int ttl, Integer priority) {
        String t = type == null ? "" : type.toUpperCase();

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

        // 4. 위협정보 평판
        threatIntelService.checkTarget(t, content);
    }
}
