package com.domainreg.worker;

import com.domainreg.core.entity.DnsRecord;
import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.enums.DomainStatus;
import com.domainreg.core.enums.JobStatus;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.port.PlatformDomainRepository;
import com.domainreg.core.port.RegistrarClient;
import com.domainreg.core.port.RegistrarJobRepository;
import com.domainreg.core.vo.Nameserver;
import com.domainreg.core.vo.RegisterCommand;
import com.domainreg.core.vo.RegisterResult;
import com.domainreg.persistence.mapper.DnsRecordMapper;
import com.domainreg.persistence.mapper.NameserverMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RegistrarJobWorker {

    private static final Logger log = LoggerFactory.getLogger(RegistrarJobWorker.class);
    private static final int BATCH_SIZE = 10;

    private final RegistrarJobRepository jobRepository;
    private final DomainRepository domainRepository;
    private final RegistrarClient registrarClient;
    private final NameserverMapper nameserverMapper;
    private final DnsRecordMapper dnsRecordMapper;
    private final PlatformDomainRepository platformDomainRepository;

    public RegistrarJobWorker(RegistrarJobRepository jobRepository,
                               DomainRepository domainRepository,
                               RegistrarClient registrarClient,
                               NameserverMapper nameserverMapper,
                               DnsRecordMapper dnsRecordMapper,
                               PlatformDomainRepository platformDomainRepository) {
        this.jobRepository = jobRepository;
        this.domainRepository = domainRepository;
        this.registrarClient = registrarClient;
        this.nameserverMapper = nameserverMapper;
        this.dnsRecordMapper = dnsRecordMapper;
        this.platformDomainRepository = platformDomainRepository;
    }

    @Scheduled(fixedDelay = 5000)
    public void processJobs() {
        List<RegistrarJob> jobs = jobRepository.findPending(BATCH_SIZE);
        for (RegistrarJob job : jobs) {
            try {
                jobRepository.updateStatus(job.getId(), JobStatus.PROCESSING);
                processJob(job);
                jobRepository.updateStatus(job.getId(), JobStatus.COMPLETED);
            } catch (Exception e) {
                log.error("Job {} failed: {}", job.getId(), e.getMessage());
                handleFailure(job, e);
            }
        }
    }

    private void processJob(RegistrarJob job) {
        switch (job.getJobType()) {
            case REGISTER -> handleRegister(job);
            case UPDATE_NS -> handleUpdateNs(job);
            case SET_PRIVACY -> handleSetPrivacy(job);
            case SYNC_DNS -> handleSyncDns(job);
        }
    }

    private void handleRegister(RegistrarJob job) {
        Domain domain = domainRepository.findById(job.getDomainId())
            .orElseThrow(() -> new IllegalStateException("Domain not found: " + job.getDomainId()));

        // Subdomain under platform domain — already ACTIVE, no external registrar needed
        if (domain.getPlatformDomainId() != null) {
            log.info("Subdomain under platform domain {}, skipping external registration: {}",
                domain.getPlatformDomainId(), domain.getNameUnicode());
            if (domain.getStatus() != DomainStatus.ACTIVE) {
                domain.setStatus(DomainStatus.ACTIVE);
                domain.setExpiresAt(java.time.LocalDate.now().plusMonths(3));
                domainRepository.save(domain);
            }
            return;
        }

        // External domain registration (registered via external registrar)
        RegisterCommand cmd = new RegisterCommand(
            domain.getNamePunycode(),
            domain.getNameUnicode(),
            domain.getTld(),
            domain.getUserId(),
            List.of()
        );

        RegisterResult result = registrarClient.register(cmd);
        if (result.success()) {
            domain.markRegistered(result.registrarRef(), result.expiresAt());
            domainRepository.save(domain);
            log.info("Domain registered: {} (ref={})", domain.getNameUnicode(), result.registrarRef());
        } else {
            throw new RuntimeException("Registration failed: " + result.errorMessage());
        }
    }

    private void handleUpdateNs(RegistrarJob job) {
        Domain domain = domainRepository.findById(job.getDomainId())
            .orElseThrow(() -> new IllegalStateException("Domain not found: " + job.getDomainId()));

        String zoneName = resolveZoneName(domain);
        if (zoneName == null) {
            log.info("No DNS zone resolved for domain {}, skipping NS update", domain.getId());
            domainRepository.updateStatus(domain.getId(), DomainStatus.ACTIVE);
            return;
        }

        List<Nameserver> ns = nameserverMapper.findByDomainId(domain.getId());
        registrarClient.updateNameservers(zoneName, domain.getNameUnicode(), ns);
        domainRepository.updateStatus(domain.getId(), DomainStatus.ACTIVE);
        log.info("NS updated for: {} ({} nameservers)", domain.getNameUnicode(), ns.size());
    }

    private void handleSetPrivacy(RegistrarJob job) {
        Domain domain = domainRepository.findById(job.getDomainId())
            .orElseThrow(() -> new IllegalStateException("Domain not found: " + job.getDomainId()));

        if (domain.getRegistrarRef() == null) {
            log.info("No registrar ref for domain {}, skipping privacy update", domain.getId());
            return;
        }

        registrarClient.setPrivacy(domain.getRegistrarRef(), domain.isPrivacyEnabled());
        log.info("Privacy {} for: {}", domain.isPrivacyEnabled() ? "enabled" : "disabled", domain.getNameUnicode());
    }

    private void handleSyncDns(RegistrarJob job) {
        Domain domain = domainRepository.findById(job.getDomainId())
            .orElseThrow(() -> new IllegalStateException("Domain not found: " + job.getDomainId()));

        // A domain with custom nameservers is delegated away — the platform no longer
        // serves its A/MX/TXT records.
        if (!nameserverMapper.findByDomainId(domain.getId()).isEmpty()) {
            log.info("Domain {} is delegated to custom nameservers, skipping DNS sync", domain.getId());
            return;
        }

        String zoneName = resolveZoneName(domain);
        if (zoneName == null) {
            log.info("No DNS zone resolved for domain {}, skipping DNS sync", domain.getId());
            return;
        }

        List<DnsRecord> records = dnsRecordMapper.findByDomainId(domain.getId());
        registrarClient.syncDnsRecords(zoneName, domain.getNameUnicode(), records);
        log.info("DNS records synced for: {} -> zone {} ({} records)",
            domain.getNameUnicode(), zoneName, records.size());
    }

    /**
     * Resolve the Cloudflare zone that owns this domain's DNS.
     * Subdomains belong to their platform domain's zone; an externally-registered
     * domain uses its own name as the zone.
     */
    private String resolveZoneName(Domain domain) {
        if (domain.getPlatformDomainId() != null) {
            return platformDomainRepository.findById(domain.getPlatformDomainId())
                .map(PlatformDomain::getNameUnicode)
                .orElse(null);
        }
        if (domain.getRegistrarRef() != null) {
            return domain.getNameUnicode();
        }
        return null;
    }

    private void handleFailure(RegistrarJob job, Exception e) {
        int nextAttempt = job.getAttempts() + 1;
        if (nextAttempt >= job.getMaxAttempts()) {
            jobRepository.markDead(job.getId(), e.getMessage());
            // Mark domain as FAILED
            domainRepository.updateStatus(job.getDomainId(), DomainStatus.FAILED);
            log.error("Job {} marked DEAD after {} attempts", job.getId(), nextAttempt);
        } else {
            long backoffMs = (long) Math.pow(2, nextAttempt) * 1000; // 2s, 4s, 8s, 16s
            jobRepository.scheduleRetry(job.getId(), nextAttempt, e.getMessage(),
                Instant.now().plusMillis(backoffMs));
        }
    }
}
