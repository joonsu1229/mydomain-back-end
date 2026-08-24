package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.enums.DomainStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DomainMapper {
    Optional<Domain> findById(@Param("id") Long id);
    Optional<Domain> findByPunycode(@Param("punycode") String punycode);
    List<Domain> findByUserId(@Param("userId") Long userId);
    void insert(Domain domain);
    void update(Domain domain);
    void updateStatus(@Param("id") Long id, @Param("status") DomainStatus status);
    void deletePaymentsByDomainId(@Param("id") Long id);
    void deleteOrdersByDomainId(@Param("id") Long id);
    void deletePrivacyByDomainId(@Param("id") Long id);
    void deleteRegistrarJobsByDomainId(@Param("id") Long id);
    void deleteById(@Param("id") Long id);
    boolean existsByPunycode(@Param("punycode") String punycode);
    boolean existsByPunycodeAndPlatform(@Param("punycode") String punycode, @Param("platformDomainId") Long platformDomainId);
    List<Domain> findByPlatformDomainId(@Param("platformDomainId") Long platformDomainId);
    long countAll();
    long countByStatus(@Param("status") String status);
    List<Domain> findAllWithUser();
}
