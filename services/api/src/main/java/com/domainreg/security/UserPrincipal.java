package com.domainreg.security;

public class UserPrincipal {
    private final Long userId;
    private final String loginId;
    private final String email;
    private final String role;

    public UserPrincipal(Long userId, String loginId, String email, String role) {
        this.userId = userId;
        this.loginId = loginId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getLoginId() { return loginId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
