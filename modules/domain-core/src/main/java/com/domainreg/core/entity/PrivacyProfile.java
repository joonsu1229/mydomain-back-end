package com.domainreg.core.entity;

import java.time.Instant;

public class PrivacyProfile {
    private Long id;
    private Long domainId;
    private String proxyEmail;
    private String proxyPhone;
    private Instant enabledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static PrivacyProfile create(Long domainId, String proxyEmail, String proxyPhone) {
        PrivacyProfile p = new PrivacyProfile();
        p.domainId = domainId;
        p.proxyEmail = proxyEmail;
        p.proxyPhone = proxyPhone;
        p.enabledAt = Instant.now();
        return p;
    }

    // -- getters / setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDomainId() { return domainId; }
    public void setDomainId(Long domainId) { this.domainId = domainId; }

    public String getProxyEmail() { return proxyEmail; }
    public void setProxyEmail(String proxyEmail) { this.proxyEmail = proxyEmail; }

    public String getProxyPhone() { return proxyPhone; }
    public void setProxyPhone(String proxyPhone) { this.proxyPhone = proxyPhone; }

    public Instant getEnabledAt() { return enabledAt; }
    public void setEnabledAt(Instant enabledAt) { this.enabledAt = enabledAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
