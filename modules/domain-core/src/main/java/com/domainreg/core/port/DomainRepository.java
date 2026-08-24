package com.domainreg.core.port;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.enums.DomainStatus;

import java.util.List;
import java.util.Optional;

public interface DomainRepository {
    Optional<Domain> findById(Long id);
    Optional<Domain> findByPunycode(String punycode);
    List<Domain> findByUserId(Long userId);
    Domain save(Domain domain);
    void updateStatus(Long id, DomainStatus status);
    void delete(Long id);
    boolean existsByPunycode(String punycode);
    boolean existsByPunycodeAndPlatform(String punycode, Long platformDomainId);
    List<Domain> findByPlatformDomainId(Long platformDomainId);
}
