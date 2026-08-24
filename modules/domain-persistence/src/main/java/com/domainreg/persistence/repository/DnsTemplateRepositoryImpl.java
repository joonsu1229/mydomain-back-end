package com.domainreg.persistence.repository;

import com.domainreg.core.entity.DnsTemplate;
import com.domainreg.core.port.DnsTemplateRepository;
import com.domainreg.persistence.mapper.DnsTemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DnsTemplateRepositoryImpl implements DnsTemplateRepository {

    private final DnsTemplateMapper mapper;

    public DnsTemplateRepositoryImpl(DnsTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<DnsTemplate> findByUserId(Long userId) {
        return mapper.findByUserId(userId);
    }

    @Override
    public Optional<DnsTemplate> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public DnsTemplate save(DnsTemplate template) {
        if (template.getId() == null) {
            mapper.insert(template);
        } else {
            mapper.update(template);
        }
        return template;
    }

    @Override
    public void deleteById(Long id) {
        mapper.delete(id);
    }
}
