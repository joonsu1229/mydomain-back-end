package com.domainreg.core.entity;

import com.domainreg.core.enums.JobStatus;
import com.domainreg.core.enums.JobType;
import java.time.Instant;

public class RegistrarJob {
    private Long id;
    private Long domainId;
    private JobType jobType;
    private String payload;
    private JobStatus status;
    private int attempts;
    private int maxAttempts;
    private String lastError;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static RegistrarJob create(Long domainId, JobType jobType, String payload) {
        RegistrarJob j = new RegistrarJob();
        j.domainId = domainId;
        j.jobType = jobType;
        j.payload = payload;
        j.status = JobStatus.PENDING;
        j.attempts = 0;
        j.maxAttempts = 5;
        return j;
    }

    // -- getters / setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDomainId() { return domainId; }
    public void setDomainId(Long domainId) { this.domainId = domainId; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
