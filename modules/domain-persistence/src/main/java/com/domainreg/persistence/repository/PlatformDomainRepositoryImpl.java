package com.domainreg.persistence.repository;

import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.port.PlatformDomainRepository;
import com.domainreg.persistence.mapper.PlatformDomainMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlatformDomainRepositoryImpl implements PlatformDomainRepository {

    private final PlatformDomainMapper mapper;

    public PlatformDomainRepositoryImpl(PlatformDomainMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PlatformDomain> findAll() {
        return mapper.findAll();
    }

    @Override
    public List<PlatformDomain> findAllActive() {
        return mapper.findAllActive();
    }

    @Override
    public Optional<PlatformDomain> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Optional<PlatformDomain> findByPunycode(String punycode) {
        return mapper.findByPunycode(punycode);
    }

    @Override
    public void save(PlatformDomain platformDomain) {
        mapper.insert(platformDomain);
    }

    @Override
    public void update(PlatformDomain platformDomain) {
        mapper.update(platformDomain);
    }

    @Override
    public void delete(Long id) {
        mapper.delete(id);
    }

    @Override
    public void hardDelete(Long id) {
        mapper.hardDeleteById(id);
    }

    @Override
    public void detachDomains(Long id) {
        mapper.detachDomains(id);
    }

    @Override
    public long countAll() {
        return mapper.countAll();
    }
}
