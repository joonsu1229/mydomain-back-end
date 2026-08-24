package com.domainreg.core.vo;

import jakarta.validation.constraints.NotBlank;

public record Nameserver(
    @NotBlank String host,
    String ip
) {}
