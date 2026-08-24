package com.domainreg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull Long platformDomainId,
    @NotBlank String prefix,
    String productType
) {}
