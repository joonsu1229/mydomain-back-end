package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.DnsTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DnsTemplateMapper {
    List<DnsTemplate> findByUserId(@Param("userId") Long userId);
    Optional<DnsTemplate> findById(@Param("id") Long id);
    void insert(DnsTemplate template);
    void update(DnsTemplate template);
    void delete(@Param("id") Long id);
}
