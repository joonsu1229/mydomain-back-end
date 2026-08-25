package com.domainreg.core.port;

import com.domainreg.core.entity.BlocklistKeyword;

import java.util.List;
import java.util.Optional;

public interface BlocklistKeywordRepository {
    List<BlocklistKeyword> findAll();
    List<BlocklistKeyword> findAllEnabled();
    Optional<BlocklistKeyword> findById(Long id);
    boolean existsByKeyword(String keyword);
    BlocklistKeyword save(BlocklistKeyword keyword);
    void deleteById(Long id);
}
