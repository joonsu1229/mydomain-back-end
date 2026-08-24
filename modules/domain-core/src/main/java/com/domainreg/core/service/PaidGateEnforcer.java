package com.domainreg.core.service;

import com.domainreg.core.entity.Domain;

public class PaidGateEnforcer {

    public void requirePaid(Domain domain) {
        if (!domain.isPaid()) {
            throw new PaymentRequiredException(
                "PRIVACY_OR_NS_REQUIRES_PAYMENT",
                "네임서버 변경과 Privacy 모드는 결제 완료된 도메인만 사용할 수 있습니다."
            );
        }
    }

    public static class PaymentRequiredException extends RuntimeException {
        private final String code;

        public PaymentRequiredException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}
