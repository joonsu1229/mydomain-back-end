package com.domainreg.core.port;

import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.enums.JobStatus;

import java.time.Instant;
import java.util.List;

public interface RegistrarJobRepository {
    RegistrarJob save(RegistrarJob job);
    List<RegistrarJob> findPending(int limit);
    void updateStatus(Long id, JobStatus status);
    void scheduleRetry(Long id, int attempts, String errorMessage, Instant nextRetryAt);
    void markDead(Long id, String errorMessage);
}
