package com.domainreg.persistence.mapper;

import com.domainreg.core.vo.Nameserver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NameserverMapper {
    List<Nameserver> findByDomainId(@Param("domainId") Long domainId);
    void insert(@Param("domainId") Long domainId, @Param("ns") Nameserver ns);
    void deleteByDomainId(@Param("domainId") Long domainId);
}
