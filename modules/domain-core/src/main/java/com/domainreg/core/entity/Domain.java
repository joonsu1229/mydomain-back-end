package com.domainreg.core.entity;

import com.domainreg.core.enums.DomainStatus;
import java.time.Instant;
import java.time.LocalDate;

public class Domain {
    private Long id;
    private Long userId;
    private String nameUnicode;
    private String namePunycode;
    private String tld;
    private Long platformDomainId;
    private DomainStatus status;
    private String registrarRef;
    private LocalDate expiresAt;
    private boolean privacyEnabled;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;

    // -- factory --

    public static Domain createReserved(Long userId, String nameUnicode, String namePunycode, String tld, Long platformDomainId) {
        Domain d = new Domain();
        d.userId = userId;
        d.nameUnicode = nameUnicode;
        d.namePunycode = namePunycode;
        d.tld = tld;
        d.platformDomainId = platformDomainId;
        d.status = DomainStatus.RESERVED;
        d.privacyEnabled = false;
        return d;
    }

    /**
     * Create a free subdomain that is immediately active (no external registrar needed).
     */
    public static Domain createFreeSubdomain(Long userId, Long platformDomainId, String nameUnicode, String namePunycode, String tld) {
        Domain d = new Domain();
        d.userId = userId;
        d.nameUnicode = nameUnicode;
        d.namePunycode = namePunycode;
        d.tld = tld;
        d.platformDomainId = platformDomainId;
        d.status = DomainStatus.ACTIVE;
        d.privacyEnabled = false;
        d.createdAt = Instant.now();
        d.expiresAt = LocalDate.now().plusMonths(3); // first term: 3 months
        return d;
    }

    // -- business methods --

    public boolean isPaid() {
        return paidAt != null && status == DomainStatus.ACTIVE;
    }

    public boolean canManageNameservers() {
        return isPaid();
    }

    public boolean canTogglePrivacy() {
        return isPaid();
    }

    public void markRegistered(String registrarRef, LocalDate expiresAt) {
        this.registrarRef = registrarRef;
        this.expiresAt = expiresAt;
        this.status = DomainStatus.ACTIVE;
        this.paidAt = Instant.now();
    }

    // -- getters / setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNameUnicode() { return nameUnicode; }
    public void setNameUnicode(String nameUnicode) { this.nameUnicode = nameUnicode; }

    public String getNamePunycode() { return namePunycode; }
    public void setNamePunycode(String namePunycode) { this.namePunycode = namePunycode; }

    public String getTld() { return tld; }
    public void setTld(String tld) { this.tld = tld; }

    public Long getPlatformDomainId() { return platformDomainId; }
    public void setPlatformDomainId(Long platformDomainId) { this.platformDomainId = platformDomainId; }

    public DomainStatus getStatus() { return status; }
    public void setStatus(DomainStatus status) { this.status = status; }

    public String getRegistrarRef() { return registrarRef; }
    public void setRegistrarRef(String registrarRef) { this.registrarRef = registrarRef; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    public boolean isPrivacyEnabled() { return privacyEnabled; }
    public void setPrivacyEnabled(boolean privacyEnabled) { this.privacyEnabled = privacyEnabled; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Domain{id=" + id + ", name='" + nameUnicode + "', status=" + status + '}';
    }
}
