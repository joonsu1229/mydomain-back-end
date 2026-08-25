package com.domainreg.core.port;

import com.domainreg.core.entity.ThreatIoc;

import java.util.Optional;

public interface ThreatIocRepository {
    Optional<ThreatIoc> findByValue(String value);
    ThreatIoc save(ThreatIoc ioc);
    void upsert(ThreatIoc ioc);
    void deleteBySource(String source);
    int countBySource(String source);
}
