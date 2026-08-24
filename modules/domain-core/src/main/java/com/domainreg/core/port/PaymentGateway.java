package com.domainreg.core.port;

public interface PaymentGateway {

    PaymentConfirmation confirm(String paymentKey, String orderId, int amount);

    record PaymentConfirmation(
        boolean success,
        String paymentKey,
        String orderId,
        int amount,
        String message
    ) {
        public static PaymentConfirmation success(String paymentKey, String orderId, int amount) {
            return new PaymentConfirmation(true, paymentKey, orderId, amount, null);
        }

        public static PaymentConfirmation failure(String message) {
            return new PaymentConfirmation(false, null, null, 0, message);
        }
    }
}
