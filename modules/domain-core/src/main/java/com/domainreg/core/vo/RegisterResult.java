package com.domainreg.core.vo;

import java.time.LocalDate;

public record RegisterResult(
    boolean success,
    String registrarRef,
    LocalDate expiresAt,
    String errorMessage
) {
    public static RegisterResult success(String registrarRef, LocalDate expiresAt) {
        return new RegisterResult(true, registrarRef, expiresAt, null);
    }

    public static RegisterResult failure(String errorMessage) {
        return new RegisterResult(false, null, null, errorMessage);
    }
}
