package com.domainreg.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9]+$",
        message = "아이디는 영문자와 숫자만 사용할 수 있습니다") String loginId,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 20) String phone,
    @NotNull Long termsId,
    @NotNull Long privacyId
) {}
