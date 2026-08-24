package com.domainreg.persistence.repository;

import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.enums.JobStatus;
import com.domainreg.core.port.RegistrarJobRepository;
import com.domainreg.persistence.mapper.RegistrarJobMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class RegistrarJobRepositoryImpl implements RegistrarJobRepository {

    private final RegistrarJobMapper mapper;

    public RegistrarJobRepositoryImpl(RegistrarJobMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public RegistrarJob save(RegistrarJob job) {
        if (job.getId() == null) {
            mapper.insert(job);
        }
        return job;
    }

    @Override
    public List<RegistrarJob> findPending(int limit) {
        return mapper.findPending(limit);
    }

    @Override
    public void updateStatus(Long id, JobStatus status) {
        mapper.updateStatus(id, status);
    }

    @Override
    public void scheduleRetry(Long id, int attempts, String errorMessage, Instant nextRetryAt) {
        mapper.scheduleRetry(id, attempts, errorMessage, nextRetryAt);
    }

    @Override
    public void markDead(Long id, String errorMessage) {
        mapper.markDead(id, errorMessage);
    }
}
