package com.domainreg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.") String password
) {}
