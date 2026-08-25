package com.domainreg.persistence.repository;

import com.domainreg.core.entity.ThreatIoc;
import com.domainreg.core.port.ThreatIocRepository;
import com.domainreg.persistence.mapper.ThreatIocMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ThreatIocRepositoryImpl implements ThreatIocRepository {

    private final ThreatIocMapper mapper;

    public ThreatIocRepositoryImpl(ThreatIocMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<ThreatIoc> findByValue(String value) {
        return mapper.findByValue(value);
    }

    @Override
    public ThreatIoc save(ThreatIoc ioc) {
        mapper.insert(ioc);
        return ioc;
    }

    @Override
    public void upsert(ThreatIoc ioc) {
        mapper.upsert(ioc);
    }

    @Override
    public void deleteBySource(String source) {
        mapper.deleteBySource(source);
    }

    @Override
    public int countBySource(String source) {
        return mapper.countBySource(source);
    }
}
