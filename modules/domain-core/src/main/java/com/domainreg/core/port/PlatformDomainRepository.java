package com.domainreg.core.port;

import com.domainreg.core.entity.PlatformDomain;

import java.util.List;
import java.util.Optional;

public interface PlatformDomainRepository {
    List<PlatformDomain> findAll();
    List<PlatformDomain> findAllActive();
    Optional<PlatformDomain> findById(Long id);
    Optional<PlatformDomain> findByPunycode(String punycode);
    void save(PlatformDomain platformDomain);
    void update(PlatformDomain platformDomain);
    void delete(Long id);
    void hardDelete(Long id);
    void detachDomains(Long id);
    long countAll();
}
