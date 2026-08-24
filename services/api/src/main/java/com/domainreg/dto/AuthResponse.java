package com.domainreg.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
