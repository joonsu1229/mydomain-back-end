package com.domainreg.worker;

import com.domainreg.core.entity.ThreatIoc;
import com.domainreg.core.port.ThreatIocRepository;
import com.domainreg.service.AppSettingsService;
import com.domainreg.threatintel.CtasClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * C-TAS 위협정보 주기 동기화: 1시간마다 IOC 목록을 받아 {@code threat_iocs}에 upsert.
 * C-TAS가 비활성화(api-key/base-url 미설정)면 아무것도 하지 않는다.
 */
@Component
public class CtasThreatSyncWorker {

    private static final Logger log = LoggerFactory.getLogger(CtasThreatSyncWorker.class);

    private final CtasClient ctasClient;
    private final AppSettingsService settings;
    private final ThreatIocRepository threatIocRepository;

    public CtasThreatSyncWorker(CtasClient ctasClient,
                                AppSettingsService settings,
                                ThreatIocRepository threatIocRepository) {
        this.ctasClient = ctasClient;
        this.settings = settings;
        this.threatIocRepository = threatIocRepository;
    }

    @Scheduled(fixedDelay = 3600_000, initialDelay = 30_000)
    public void sync() {
        if (!ctasEnabled()) {
            return;
        }
        try {
            List<ThreatIoc> iocs = ctasClient.fetchIocs();
            for (ThreatIoc ioc : iocs) {
                threatIocRepository.upsert(ioc);
            }
            log.info("C-TAS threat sync: {} IOCs upserted", iocs.size());
        } catch (Exception e) {
            log.warn("C-TAS threat sync failed: {}", e.getMessage());
        }
    }

    private boolean ctasEnabled() {
        return !settings.getOrDefault("ctas.api-key", "").isBlank()
            && !settings.getOrDefault("ctas.base-url", "").isBlank();
    }
}
