package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.DnsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DnsRecordMapper {
    List<DnsRecord> findByDomainId(@Param("domainId") Long domainId);
    Optional<DnsRecord> findById(@Param("id") Long id);
    void insert(DnsRecord record);
    void update(DnsRecord record);
    void delete(@Param("id") Long id);
}
