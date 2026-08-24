package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.PlatformDomain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PlatformDomainMapper {
    Optional<PlatformDomain> findById(@Param("id") Long id);
    Optional<PlatformDomain> findByPunycode(@Param("punycode") String punycode);
    List<PlatformDomain> findAll();
    List<PlatformDomain> findAllActive();
    void insert(PlatformDomain platformDomain);
    void update(PlatformDomain platformDomain);
    void delete(@Param("id") Long id);
    void hardDeleteById(@Param("id") Long id);
    void detachDomains(@Param("id") Long id);
    long countAll();
}
