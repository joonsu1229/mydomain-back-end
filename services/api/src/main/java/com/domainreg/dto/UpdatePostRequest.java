package com.domainreg.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostRequest(
    @NotBlank String title,
    @NotBlank String content,
    String password,      // 새 비밀번호(선택) — 비밀글 전환/유지 시
    Boolean isSecret
) {}
