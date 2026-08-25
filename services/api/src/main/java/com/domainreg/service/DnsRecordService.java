package com.domainreg.service;

import com.domainreg.core.entity.DnsRecord;
import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.enums.JobType;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.port.RegistrarJobRepository;
import com.domainreg.exception.SecurityPolicyException;
import com.domainreg.persistence.mapper.DnsRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class DnsRecordService {

    private static final Set<String> VALID_TYPES = Set.of("A","AAAA","CNAME","MX","TXT");

    private final DnsRecordMapper mapper;
    private final DomainRepository domainRepository;
    private final RegistrarJobRepository jobRepository;
    private final SecurityPolicyService securityPolicyService;

    public DnsRecordService(DnsRecordMapper mapper,
                            DomainRepository domainRepository,
                            RegistrarJobRepository jobRepository,
                            SecurityPolicyService securityPolicyService) {
        this.mapper = mapper;
        this.domainRepository = domainRepository;
        this.jobRepository = jobRepository;
        this.securityPolicyService = securityPolicyService;
    }

    public List<DnsRecord> getRecords(Long userId, Long domainId) {
        verifyOwnership(userId, domainId);
        return mapper.findByDomainId(domainId);
    }

    @Transactional
    public DnsRecord addRecord(Long userId, Long domainId, String type, String name,
                                String content, int ttl, Integer priority) {
        verifyOwnership(userId, domainId);
        String upper = type == null ? "" : type.toUpperCase();
        if (!VALID_TYPES.contains(upper)) {
            throw new SecurityPolicyException("INVALID_TYPE", "지원하지 않는 레코드 타입입니다: " + type);
        }
        securityPolicyService.validateRecord(upper, name, content, ttl, priority);
        DnsRecord r = DnsRecord.create(domainId, upper, name, content, ttl, priority);
        mapper.insert(r);
        enqueueSyncJob(domainId);
        return r;
    }

    @Transactional
    public DnsRecord updateRecord(Long userId, Long domainId, Long recordId,
                                   String type, String name, String content, int ttl, Integer priority) {
        verifyOwnership(userId, domainId);
        DnsRecord r = mapper.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found"));
        if (!r.getDomainId().equals(domainId)) {
            throw new IllegalArgumentException("Record does not belong to domain");
        }
        String upper = type == null ? "" : type.toUpperCase();
        if (!VALID_TYPES.contains(upper)) {
            throw new SecurityPolicyException("INVALID_TYPE", "지원하지 않는 레코드 타입입니다: " + type);
        }
        securityPolicyService.validateRecord(upper, name, content, ttl, priority);
        r.setRecordType(upper);
        r.setName(name);
        r.setContent(content);
        r.setTtl(ttl);
        r.setPriority(priority);
        mapper.update(r);
        enqueueSyncJob(domainId);
        return r;
    }

    @Transactional
    public void deleteRecord(Long userId, Long domainId, Long recordId) {
        verifyOwnership(userId, domainId);
        DnsRecord r = mapper.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found"));
        if (!r.getDomainId().equals(domainId)) {
            throw new IllegalArgumentException("Record does not belong to domain");
        }
        mapper.delete(recordId);
        enqueueSyncJob(domainId);
    }

    private void enqueueSyncJob(Long domainId) {
        Domain d = domainRepository.findById(domainId).orElse(null);
        if (d != null && (d.getRegistrarRef() != null || d.getPlatformDomainId() != null)) {
            RegistrarJob job = RegistrarJob.create(domainId, JobType.SYNC_DNS,
                "{\"domainId\":" + domainId + "}");
            jobRepository.save(job);
        }
    }

    private void verifyOwnership(Long userId, Long domainId) {
        Domain d = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found"));
        if (!d.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
    }
}
