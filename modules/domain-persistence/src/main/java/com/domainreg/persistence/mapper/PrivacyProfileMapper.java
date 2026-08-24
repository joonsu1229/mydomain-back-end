package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.PrivacyProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface PrivacyProfileMapper {
    Optional<PrivacyProfile> findByDomainId(@Param("domainId") Long domainId);
    void insert(PrivacyProfile profile);
    void update(PrivacyProfile profile);
    void deleteByDomainId(@Param("domainId") Long domainId);
}
