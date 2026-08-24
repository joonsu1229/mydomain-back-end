package com.domainreg.core.port;

import com.domainreg.core.entity.DnsTemplate;
import java.util.List;
import java.util.Optional;

public interface DnsTemplateRepository {
    List<DnsTemplate> findByUserId(Long userId);
    Optional<DnsTemplate> findById(Long id);
    DnsTemplate save(DnsTemplate template);
    void deleteById(Long id);
}
