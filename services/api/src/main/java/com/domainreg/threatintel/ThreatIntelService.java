package com.domainreg.threatintel;

import com.domainreg.core.entity.ThreatIoc;
import com.domainreg.core.port.ThreatIocRepository;
import com.domainreg.exception.SecurityPolicyException;
import com.domainreg.service.AppSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A/CNAME 대상에 대한 위협정보 평판 검사 오케스트레이션.
 * 외부 검사는 모두 fail-open(실패 시 차단하지 않음). 적발 시 {@code BLOCKED_TARGET}.
 */
@Component
public class ThreatIntelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelService.class);

    private final SpamhausDnsblChecker dnsbl;
    private final GoogleSafeBrowsingClient safeBrowsing;
    private final AppSettingsService settings;
    private final ThreatIocRepository threatIocRepository;

    public ThreatIntelService(SpamhausDnsblChecker dnsbl,
                              GoogleSafeBrowsingClient safeBrowsing,
                              AppSettingsService settings,
                              ThreatIocRepository threatIocRepository) {
        this.dnsbl = dnsbl;
        this.safeBrowsing = safeBrowsing;
        this.settings = settings;
        this.threatIocRepository = threatIocRepository;
    }

    private boolean ctasEnabled() {
        return !settings.getOrDefault("ctas.api-key", "").isBlank()
            && !settings.getOrDefault("ctas.base-url", "").isBlank();
    }

    public void checkTarget(String type, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String t = type == null ? "" : type.toUpperCase();

        if ("A".equals(t)) {
            if (dnsbl.isListed(content)) {
                throw new SecurityPolicyException("BLOCKED_TARGET",
                    "연결하려는 주소가 보안상 위험하여 등록할 수 없습니다. (사유: Spamhaus DNSBL)");
            }
            if (safeBrowsing.isThreatUrl("http://" + content + "/")) {
                throw new SecurityPolicyException("BLOCKED_TARGET",
                    "연결하려는 주소가 보안상 위험하여 등록할 수 없습니다. (사유: Google Safe Browsing)");
            }
        } else if ("CNAME".equals(t)) {
            if (safeBrowsing.isThreatUrl("http://" + content + "/")) {
                throw new SecurityPolicyException("BLOCKED_TARGET",
                    "연결하려는 주소가 보안상 위험하여 등록할 수 없습니다. (사유: Google Safe Browsing)");
            }
        } else if ("AAAA".equals(t)) {
            // IPv6는 DNSBL/Safe Browsing URL 검사가 적합하지 않아 C-TAS 캐시 조회만 수행
        }

        // C-TAS 캐시(주기적 동기화 IOC) 조회 — IP/도메인 공통
        if (ctasEnabled()) {
            ThreatIoc ioc = threatIocRepository.findByValue(content).orElse(null);
            if (ioc != null) {
                throw new SecurityPolicyException("BLOCKED_TARGET",
                    "연결하려는 주소가 보안상 위험하여 등록할 수 없습니다. (사유: KISA C-TAS)");
            }
        }
    }
}
