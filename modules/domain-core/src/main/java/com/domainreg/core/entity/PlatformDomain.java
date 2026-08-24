package com.domainreg.core.entity;

import java.time.Instant;

/**
 * A root domain owned by the platform (e.g., yourhost.kr, kro.kr).
 * Users create subdomains under these platform domains.
 */
public class PlatformDomain {
    private Long id;
    private String nameUnicode;
    private String namePunycode;
    private String displayName;
    private String description;
    private String status;
    private String registrarRef;
    private String nsDefault;
    private String verificationToken;
    private Instant verifiedAt;
    private Instant expiresAt;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNameUnicode() { return nameUnicode; }
    public void setNameUnicode(String nameUnicode) { this.nameUnicode = nameUnicode; }

    public String getNamePunycode() { return namePunycode; }
    public void setNamePunycode(String namePunycode) { this.namePunycode = namePunycode; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRegistrarRef() { return registrarRef; }
    public void setRegistrarRef(String registrarRef) { this.registrarRef = registrarRef; }

    public String getNsDefault() { return nsDefault; }
    public void setNsDefault(String nsDefault) { this.nsDefault = nsDefault; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
