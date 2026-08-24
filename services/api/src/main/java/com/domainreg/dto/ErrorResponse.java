package com.domainreg.dto;

public record ErrorResponse(
    String code,
    String message
) {}
