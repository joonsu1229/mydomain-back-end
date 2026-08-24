package com.domainreg.controller;

import jakarta.validation.Valid;
import com.domainreg.dto.PaymentConfirmRequest;
import com.domainreg.dto.PaymentConfirmResponse;
import com.domainreg.core.entity.Payment;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @Valid @RequestBody PaymentConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Payment payment = paymentService.confirmPayment(
            principal.getUserId(),
            request.paymentKey(),
            request.orderId(),
            request.amount()
        );

        return ResponseEntity.ok(new PaymentConfirmResponse(
            payment.getId(),
            payment.getOrderId(),
            null, // domainId resolved by worker
            "CONFIRMED",
            "결제가 완료되었습니다. 도메인 등록이 곧 처리됩니다."
        ));
    }
}
