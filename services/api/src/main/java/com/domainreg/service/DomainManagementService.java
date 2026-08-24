package com.domainreg.service;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.entity.PrivacyProfile;
import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.entity.User;
import com.domainreg.core.enums.DomainStatus;
import com.domainreg.core.enums.JobType;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.port.PlatformDomainRepository;
import com.domainreg.core.port.RegistrarClient;
import com.domainreg.core.port.RegistrarJobRepository;
import com.domainreg.core.port.UserRepository;
import com.domainreg.core.service.DomainStateMachine;
import com.domainreg.core.service.PaidGateEnforcer;
import com.domainreg.core.vo.Nameserver;
import com.domainreg.persistence.mapper.NameserverMapper;
import com.domainreg.persistence.mapper.PrivacyProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class DomainManagementService {

    private static final Logger log = LoggerFactory.getLogger(DomainManagementService.class);

    private final DomainRepository domainRepository;
    private final NameserverMapper nameserverMapper;
    private final PrivacyProfileMapper privacyMapper;
    private final RegistrarJobRepository jobRepository;
    private final RegistrarClient registrarClient;
    private final DomainStateMachine stateMachine;
    private final PaidGateEnforcer paidGate;
    private final PlatformDomainRepository platformDomainRepository;
    private final UserRepository userRepository;

    public DomainManagementService(DomainRepository domainRepository,
                                    NameserverMapper nameserverMapper,
                                    PrivacyProfileMapper privacyMapper,
                                    RegistrarJobRepository jobRepository,
                                    RegistrarClient registrarClient,
                                    DomainStateMachine stateMachine,
                                    PaidGateEnforcer paidGate,
                                    PlatformDomainRepository platformDomainRepository,
                                    UserRepository userRepository) {
        this.domainRepository = domainRepository;
        this.nameserverMapper = nameserverMapper;
        this.privacyMapper = privacyMapper;
        this.jobRepository = jobRepository;
        this.registrarClient = registrarClient;
        this.stateMachine = stateMachine;
        this.paidGate = paidGate;
        this.platformDomainRepository = platformDomainRepository;
        this.userRepository = userRepository;
    }

    // -- Nameserver management --

    public List<Nameserver> getNameservers(Long domainId) {
        return nameserverMapper.findByDomainId(domainId);
    }

    @Transactional
    public void updateNameservers(Long userId, Long domainId, List<Nameserver> nameservers) {
        Domain domain = getOwnedDomain(userId, domainId);
        requireNs(userId);

        // Transition state
        stateMachine.transition(domain, DomainStatus.NS_UPDATING);
        domainRepository.updateStatus(domainId, DomainStatus.NS_UPDATING);

        // Update nameservers in DB
        nameserverMapper.deleteByDomainId(domainId);
        for (Nameserver ns : nameservers) {
            nameserverMapper.insert(domainId, ns);
        }

        // Enqueue registrar job if the domain has a resolvable DNS zone — either a
        // subdomain under a platform domain, or an externally-registered domain.
        if (domain.getRegistrarRef() != null || domain.getPlatformDomainId() != null) {
            String payload = "{\"domainId\":" + domainId + "}";
            RegistrarJob job = RegistrarJob.create(domainId, JobType.UPDATE_NS, payload);
            jobRepository.save(job);
        } else {
            // No zone to push to: just go back to ACTIVE
            domainRepository.updateStatus(domainId, DomainStatus.ACTIVE);
        }
    }

    // -- Privacy management --

    public boolean getPrivacyStatus(Long domainId) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found"));
        return domain.isPrivacyEnabled();
    }

    @Transactional
    public void togglePrivacy(Long userId, Long domainId, boolean enabled) {
        Domain domain = getOwnedDomain(userId, domainId);
        requirePrivacy(userId);

        if (enabled) {
            stateMachine.transition(domain, DomainStatus.PRIVACY_ON);
            domainRepository.updateStatus(domainId, DomainStatus.PRIVACY_ON);

            // Create/update privacy profile with proxy info
            String proxyEmail = "privacy-" + domainId + "@proxy.domain-kr.com";
            String proxyPhone = "02-0000-" + String.format("%04d", domainId % 10000);

            var existing = privacyMapper.findByDomainId(domainId);
            if (existing.isPresent()) {
                PrivacyProfile p = existing.get();
                // only update if re-enabling
                privacyMapper.update(p);
            } else {
                PrivacyProfile p = PrivacyProfile.create(domainId, proxyEmail, proxyPhone);
                privacyMapper.insert(p);
            }
        } else {
            stateMachine.transition(domain, DomainStatus.ACTIVE);
            domainRepository.updateStatus(domainId, DomainStatus.ACTIVE);
            privacyMapper.deleteByDomainId(domainId);
        }

        // Update domain privacy flag
        domain.setPrivacyEnabled(enabled);
        domainRepository.save(domain);

        // Enqueue registrar job
        if (domain.getRegistrarRef() != null) {
            String payload = "{\"domainId\":" + domainId + ",\"enabled\":" + enabled + "}";
            RegistrarJob job = RegistrarJob.create(domainId, JobType.SET_PRIVACY, payload);
            jobRepository.save(job);
        }
    }

    /**
     * Permanently delete a domain owned by the user.
     *
     * <p>Removes any DNS records this platform pushed to Cloudflare for the subdomain
     * (best-effort) so it stops resolving, then deletes the domain row. Child rows with
     * FK references (orders/payments, privacy, registrar jobs) are cleaned up inside
     * {@link DomainRepository#delete}.
     */
    @Transactional
    public void deleteDomain(Long userId, Long domainId) {
        Domain domain = getOwnedDomain(userId, domainId);
        cleanupAndDelete(domain);
    }

    /**
     * Admin delete — removes any domain regardless of ownership.
     */
    @Transactional
    public void deleteDomainAdmin(Long domainId) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다."));
        cleanupAndDelete(domain);
    }

    private void cleanupAndDelete(Domain domain) {
        // Subdomains under a platform domain have their DNS records pushed to the
        // platform zone in Cloudflare. Clear them so the hostname stops resolving.
        if (domain.getPlatformDomainId() != null) {
            try {
                String zoneName = platformDomainRepository.findById(domain.getPlatformDomainId())
                    .map(PlatformDomain::getNameUnicode)
                    .orElse(null);
                registrarClient.syncDnsRecords(zoneName, domain.getNameUnicode(), List.of());
            } catch (Exception e) {
                // Best-effort — never let a Cloudflare failure block the delete.
                log.warn("Cloudflare cleanup failed for {}: {}", domain.getNameUnicode(), e.getMessage());
            }
        }

        domainRepository.delete(domain.getId());
    }

    /**
     * Renew a subdomain: extends its expiration by 6 months.
     *
     * <p>The first term is 3 months; every renewal extends by 6 months. Renewal is
     * allowed only from 1 month before expiry (already-expired domains remain
     * renewable). Returns the updated domain.
     */
    @Transactional
    public Domain renewDomain(Long userId, Long domainId) {
        Domain domain = getOwnedDomain(userId, domainId);
        LocalDate today = LocalDate.now();
        LocalDate expiry = domain.getExpiresAt() != null ? domain.getExpiresAt() : today;

        if (expiry.isAfter(today.plusMonths(1))) {
            throw new IllegalStateException("갱신은 만료 1개월 전부터 가능합니다.");
        }

        LocalDate base = expiry.isAfter(today) ? expiry : today;
        domain.setExpiresAt(base.plusMonths(6));
        domainRepository.save(domain);
        return domain;
    }

    private void requireNs(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!user.isNsEnabled() && !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("네임서버 변경은 관리자의 승인이 필요합니다.");
        }
    }

    private void requirePrivacy(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!user.isPrivacyEnabled() && !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("개인정보 보호는 관리자의 승인이 필요합니다.");
        }
    }

    private Domain getOwnedDomain(Long userId, Long domainId) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다."));
        if (!domain.getUserId().equals(userId)) {
            throw new SecurityException("해당 도메인에 접근할 수 없습니다.");
        }
        return domain;
    }
}
