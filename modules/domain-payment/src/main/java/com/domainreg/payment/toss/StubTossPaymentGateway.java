package com.domainreg.payment.toss;

import com.domainreg.core.port.PaymentGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payment.mode", havingValue = "stub", matchIfMissing = true)
public class StubTossPaymentGateway implements PaymentGateway {

    @Override
    public PaymentConfirmation confirm(String paymentKey, String orderId, int amount) {
        // Stub: always succeed
        String key = paymentKey != null ? paymentKey : "stub_pk_" + UUID.randomUUID().toString().substring(0, 12);
        return PaymentConfirmation.success(key, orderId, amount);
    }
}
