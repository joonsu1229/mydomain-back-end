package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.ThreatIoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ThreatIocMapper {
    Optional<ThreatIoc> findByValue(@Param("value") String value);
    void insert(ThreatIoc ioc);
    void upsert(ThreatIoc ioc);
    void deleteBySource(@Param("source") String source);
    int countBySource(@Param("source") String source);
}
