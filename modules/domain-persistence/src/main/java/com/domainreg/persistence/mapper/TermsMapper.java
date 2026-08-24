package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.Terms;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TermsMapper {
    List<Terms> findAll();
    List<Terms> findCurrentAll();
    Optional<Terms> findById(@Param("id") Long id);
    Integer findMaxVersion(@Param("type") String type);
    void insert(Terms terms);
    void updateContent(@Param("id") Long id, @Param("title") String title, @Param("content") String content);
    void clearCurrent(@Param("type") String type);
    void publish(@Param("id") Long id);
    void delete(@Param("id") Long id);
}
