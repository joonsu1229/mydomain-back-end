package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.enums.JobStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface RegistrarJobMapper {
    List<RegistrarJob> findPending(@Param("limit") int limit);
    void insert(RegistrarJob job);
    void updateStatus(@Param("id") Long id, @Param("status") JobStatus status);
    void scheduleRetry(@Param("id") Long id, @Param("attempts") int attempts,
                       @Param("lastError") String lastError, @Param("nextRetryAt") Instant nextRetryAt);
    void markDead(@Param("id") Long id, @Param("lastError") String lastError);
    List<RegistrarJob> findByDomainId(@Param("domainId") Long domainId);
}
