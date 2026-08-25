package com.domainreg.core.entity;

import java.time.Instant;

public class User {
    private Long id;
    private String loginId;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String role;
    private boolean nsEnabled;
    private boolean privacyEnabled;
    private int domainLimit;
    private boolean emailVerified;
    private String verificationToken;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    private String lastLoginIp;
    private Instant createdAt;
    private Instant updatedAt;

    public static User create(String loginId, String email, String passwordHash, String name, String phone) {
        User u = new User();
        u.loginId = loginId;
        u.email = email;
        u.passwordHash = passwordHash;
        u.name = name;
        u.phone = phone;
        u.role = "USER";
        u.emailVerified = false;
        u.domainLimit = 3;
        return u;
    }

    // -- getters / setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isNsEnabled() { return nsEnabled; }
    public void setNsEnabled(boolean nsEnabled) { this.nsEnabled = nsEnabled; }

    public boolean isPrivacyEnabled() { return privacyEnabled; }
    public void setPrivacyEnabled(boolean privacyEnabled) { this.privacyEnabled = privacyEnabled; }

    public int getDomainLimit() { return domainLimit; }
    public void setDomainLimit(int domainLimit) { this.domainLimit = domainLimit; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean v) { this.emailVerified = v; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String v) { this.verificationToken = v; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int v) { this.failedLoginAttempts = v; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant v) { this.lockedUntil = v; }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant v) { this.lastLoginAt = v; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String v) { this.lastLoginIp = v; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
