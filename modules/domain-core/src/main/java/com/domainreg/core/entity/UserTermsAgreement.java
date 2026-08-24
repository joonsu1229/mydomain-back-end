package com.domainreg.core.entity;

import java.time.Instant;

public class UserTermsAgreement {

    private Long id;
    private Long userId;
    private Long termsId;
    private Instant agreedAt;
    private String ipAddress;

    public static UserTermsAgreement create(Long userId, Long termsId, String ipAddress) {
        UserTermsAgreement a = new UserTermsAgreement();
        a.userId = userId;
        a.termsId = termsId;
        a.ipAddress = ipAddress;
        a.agreedAt = Instant.now();
        return a;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTermsId() { return termsId; }
    public void setTermsId(Long termsId) { this.termsId = termsId; }

    public Instant getAgreedAt() { return agreedAt; }
    public void setAgreedAt(Instant agreedAt) { this.agreedAt = agreedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
