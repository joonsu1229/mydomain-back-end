package com.domainreg.exception;

/**
 * 사전 차단/검증 위반 시 던지는 예외. {@link GlobalExceptionHandler}에서 400으로 변환된다.
 */
public class SecurityPolicyException extends RuntimeException {

    private final String code;

    public SecurityPolicyException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
