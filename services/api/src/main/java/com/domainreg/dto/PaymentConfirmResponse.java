package com.domainreg.dto;

public record PaymentConfirmResponse(
    Long paymentId,
    Long orderId,
    Long domainId,
    String status,
    String message
) {}
