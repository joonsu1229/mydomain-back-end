package com.domainreg.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(
    @NotBlank String title,
    @NotBlank String content,
    String password,      // 비밀글 비밀번호(비밀글일 때 필수)
    Boolean anonymous,    // 익명 여부
    Boolean isSecret,     // 비밀글 여부
    Boolean isNotice      // 공지 여부(관리자만 허용)
) {}
