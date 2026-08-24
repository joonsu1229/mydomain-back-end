package com.domainreg.core.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RegisterCommand(
    @NotBlank String punycodeName,
    @NotBlank String unicodeName,
    @NotBlank String tld,
    @NotNull Long userId,
    List<Nameserver> initialNameservers
) {}
