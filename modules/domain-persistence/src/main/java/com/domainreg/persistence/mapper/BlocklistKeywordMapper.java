package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.BlocklistKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BlocklistKeywordMapper {
    List<BlocklistKeyword> findAll();
    List<BlocklistKeyword> findAllEnabled();
    Optional<BlocklistKeyword> findById(@Param("id") Long id);
    int countByKeyword(@Param("keyword") String keyword);
    void insert(BlocklistKeyword keyword);
    void update(BlocklistKeyword keyword);
    void delete(@Param("id") Long id);
}
