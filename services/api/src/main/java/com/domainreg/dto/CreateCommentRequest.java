package com.domainreg.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
    @NotBlank String content,
    Long parentId        // NULL = 최상위 댓글, 값 = 대댓글(1단계만)
) {}
