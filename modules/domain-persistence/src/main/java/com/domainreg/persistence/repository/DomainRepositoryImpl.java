package com.domainreg.persistence.repository;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.enums.DomainStatus;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.persistence.mapper.DomainMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DomainRepositoryImpl implements DomainRepository {

    private final DomainMapper mapper;

    public DomainRepositoryImpl(DomainMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Domain> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Optional<Domain> findByPunycode(String punycode) {
        return mapper.findByPunycode(punycode);
    }

    @Override
    public List<Domain> findByUserId(Long userId) {
        return mapper.findByUserId(userId);
    }

    @Override
    public Domain save(Domain domain) {
        if (domain.getId() == null) {
            mapper.insert(domain);
        } else {
            mapper.update(domain);
        }
        return domain;
    }

    @Override
    public void updateStatus(Long id, DomainStatus status) {
        mapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        // Remove child rows first — registrar_jobs, orders (and their payments) and
        // privacy_profiles have no ON DELETE CASCADE and would block the domain delete.
        // domain_nameservers and dns_records cascade automatically.
        mapper.deletePaymentsByDomainId(id);
        mapper.deleteOrdersByDomainId(id);
        mapper.deletePrivacyByDomainId(id);
        mapper.deleteRegistrarJobsByDomainId(id);
        mapper.deleteById(id);
    }

    @Override
    public boolean existsByPunycode(String punycode) {
        return mapper.existsByPunycode(punycode);
    }

    @Override
    public boolean existsByPunycodeAndPlatform(String punycode, Long platformDomainId) {
        return mapper.existsByPunycodeAndPlatform(punycode, platformDomainId);
    }

    @Override
    public List<Domain> findByPlatformDomainId(Long platformDomainId) {
        return mapper.findByPlatformDomainId(platformDomainId);
    }
}
