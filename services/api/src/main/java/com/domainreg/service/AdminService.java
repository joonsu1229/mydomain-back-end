package com.domainreg.service;

import com.domainreg.core.entity.Domain;
import com.domainreg.core.entity.PlatformDomain;
import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.entity.User;
import com.domainreg.core.port.DomainRepository;
import com.domainreg.core.port.UserRepository;
import com.domainreg.persistence.mapper.DnsRecordMapper;
import com.domainreg.persistence.mapper.DomainMapper;
import com.domainreg.persistence.mapper.RegistrarJobMapper;
import com.domainreg.persistence.mapper.UserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final DomainMapper domainMapper;
    private final DnsRecordMapper dnsRecordMapper;
    private final UserMapper userMapper;
    private final RegistrarJobMapper registrarJobMapper;
    private final PlatformDomainService platformDomainService;
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final DomainManagementService domainManagementService;

    public AdminService(UserRepository userRepository,
                        DomainRepository domainRepository,
                        DomainMapper domainMapper,
                        DnsRecordMapper dnsRecordMapper,
                        UserMapper userMapper,
                        RegistrarJobMapper registrarJobMapper,
                        PlatformDomainService platformDomainService,
                        StringRedisTemplate redis,
                        PasswordEncoder passwordEncoder,
                        DomainManagementService domainManagementService) {
        this.userRepository = userRepository;
        this.domainRepository = domainRepository;
        this.domainMapper = domainMapper;
        this.dnsRecordMapper = dnsRecordMapper;
        this.userMapper = userMapper;
        this.registrarJobMapper = registrarJobMapper;
        this.platformDomainService = platformDomainService;
        this.redis = redis;
        this.passwordEncoder = passwordEncoder;
        this.domainManagementService = domainManagementService;
    }

    public Map<String, Object> getDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userMapper.countAll());
        stats.put("totalDomains", domainMapper.countAll());
        stats.put("activeDomains", domainMapper.countByStatus("ACTIVE"));
        stats.put("expiredDomains", domainMapper.countByStatus("EXPIRED"));
        stats.put("pendingPayments", domainMapper.countByStatus("PENDING_PAYMENT"));
        stats.put("totalPlatformDomains", platformDomainService.getAllPlatformDomains().size());
        return stats;
    }

    public List<PlatformDomain> getPlatformDomains() {
        return platformDomainService.getAllPlatformDomains();
    }

    public PlatformDomain addPlatformDomain(String domainName, String displayName, String description) {
        return platformDomainService.addPlatformDomain(domainName, displayName, description, null);
    }

    public PlatformDomain updatePlatformDomain(Long id, String displayName, String description, boolean isActive) {
        return platformDomainService.updatePlatformDomain(id, displayName, description, null, isActive);
    }

    public void deletePlatformDomain(Long id) {
        platformDomainService.deletePlatformDomain(id);
    }

    public List<User> getAllUsers() {
        return userMapper.findAll();
    }

    public List<Domain> getAllDomains() {
        return domainMapper.findAllWithUser();
    }

    public Map<String, Object> getDomainDetail(Long domainId) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다."));
        List<RegistrarJob> jobs = registrarJobMapper.findByDomainId(domainId);
        RegistrarJob latest = jobs.isEmpty() ? null : jobs.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("domain", domain);
        result.put("lastError", latest != null ? latest.getLastError() : null);
        result.put("jobStatus", latest != null ? latest.getStatus().name() : null);
        result.put("jobType", latest != null ? latest.getJobType().name() : null);
        result.put("records", dnsRecordMapper.findByDomainId(domainId));
        return result;
    }

    public User getUserDetail(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void updateUserRole(Long userId, String role) {
        User u = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setRole(role);
        userRepository.save(u);
        // Invalidate role cache so JWT filter picks up the change immediately
        redis.delete("role:" + userId);
    }

    public void updatePermissions(Long userId, boolean nsEnabled, boolean privacyEnabled) {
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userMapper.updatePermissions(userId, nsEnabled, privacyEnabled);
    }

    public void updateUserAccount(Long userId, String email, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean changed = false;

        // 이메일: 값이 있을 때만 변경
        if (email != null && !email.isBlank()) {
            String newEmail = email.trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userMapper.existsByEmail(newEmail)) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                userMapper.updateEmail(userId, newEmail);
                changed = true;
            }
        }

        // 비밀번호: 값이 있을 때만 변경
        if (password != null && !password.isBlank()) {
            if (password.length() < 8) {
                throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
            }
            userMapper.updatePassword(userId, passwordEncoder.encode(password));
            // Invalidate refresh tokens so the user must re-login
            redis.delete("refresh:" + userId);
            changed = true;
        }

        if (!changed) {
            throw new IllegalArgumentException("변경할 이메일 또는 비밀번호를 입력해주세요.");
        }
    }

    public void suspendUser(Long userId, boolean suspended) {
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (suspended) {
            userMapper.lockAccount(userId, Instant.now().plusSeconds(3153600000L)); // indefinite
        } else {
            userMapper.unlockAccount(userId);
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Delete the user's domains (with Cloudflare + child-table cleanup)
        for (Domain d : domainRepository.findByUserId(userId)) {
            domainManagementService.deleteDomainAdmin(d.getId());
        }
        // Delete the user's orders + payments, then the user
        userMapper.deletePaymentsByUserId(userId);
        userMapper.deleteOrdersByUserId(userId);
        redis.delete("refresh:" + userId);
        userMapper.deleteById(userId);
    }

    public void deleteDomain(Long domainId) {
        domainManagementService.deleteDomainAdmin(domainId);
    }

    /**
     * Admin renews a domain: extends its expiration by 6 months (no payment/restriction).
     */
    @Transactional
    public Domain renewDomain(Long domainId) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다."));
        LocalDate today = LocalDate.now();
        LocalDate expiry = domain.getExpiresAt() != null ? domain.getExpiresAt() : today;
        LocalDate base = expiry.isAfter(today) ? expiry : today;
        domain.setExpiresAt(base.plusMonths(6));
        domainRepository.save(domain);
        return domain;
    }
}
