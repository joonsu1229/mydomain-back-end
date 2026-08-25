package com.domainreg.persistence.repository;

import com.domainreg.core.entity.BlocklistKeyword;
import com.domainreg.core.port.BlocklistKeywordRepository;
import com.domainreg.persistence.mapper.BlocklistKeywordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BlocklistKeywordRepositoryImpl implements BlocklistKeywordRepository {

    private final BlocklistKeywordMapper mapper;

    public BlocklistKeywordRepositoryImpl(BlocklistKeywordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<BlocklistKeyword> findAll() {
        return mapper.findAll();
    }

    @Override
    public List<BlocklistKeyword> findAllEnabled() {
        return mapper.findAllEnabled();
    }

    @Override
    public Optional<BlocklistKeyword> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public boolean existsByKeyword(String keyword) {
        return mapper.countByKeyword(keyword) > 0;
    }

    @Override
    public BlocklistKeyword save(BlocklistKeyword keyword) {
        if (keyword.getId() == null) {
            mapper.insert(keyword);
        } else {
            mapper.update(keyword);
        }
        return keyword;
    }

    @Override
    public void deleteById(Long id) {
        mapper.delete(id);
    }
}
