package com.domainreg.dto;

public record CreateOrderResponse(
    Long orderId,
    String orderNumber,
    int amount,
    String currency,
    String status,
    PaymentInfo paymentInfo
) {
    public record PaymentInfo(
        String clientKey,
        String orderId,
        String orderName,
        int amount,
        String customerEmail
    ) {}
}
