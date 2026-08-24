package com.domainreg.persistence.repository;

import com.domainreg.core.entity.Terms;
import com.domainreg.core.port.TermsRepository;
import com.domainreg.persistence.mapper.TermsMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TermsRepositoryImpl implements TermsRepository {

    private final TermsMapper mapper;

    public TermsRepositoryImpl(TermsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Terms> findAll() {
        return mapper.findAll();
    }

    @Override
    public List<Terms> findCurrentAll() {
        return mapper.findCurrentAll();
    }

    @Override
    public Optional<Terms> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Integer findMaxVersion(String type) {
        return mapper.findMaxVersion(type);
    }

    @Override
    public Terms insert(Terms terms) {
        mapper.insert(terms);
        return terms;
    }

    @Override
    public void updateContent(Long id, String title, String content) {
        mapper.updateContent(id, title, content);
    }

    @Override
    public void clearCurrent(String type) {
        mapper.clearCurrent(type);
    }

    @Override
    public void publish(Long id) {
        mapper.publish(id);
    }

    @Override
    public void deleteById(Long id) {
        mapper.delete(id);
    }
}
