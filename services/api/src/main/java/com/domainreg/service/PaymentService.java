package com.domainreg.service;

import com.domainreg.core.entity.Order;
import com.domainreg.core.entity.Payment;
import com.domainreg.core.entity.RegistrarJob;
import com.domainreg.core.enums.JobType;
import com.domainreg.core.enums.OrderStatus;
import com.domainreg.core.port.*;
import com.domainreg.core.port.PaymentGateway.PaymentConfirmation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RegistrarJobRepository jobRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(OrderRepository orderRepository,
                          PaymentRepository paymentRepository,
                          RegistrarJobRepository jobRepository,
                          PaymentGateway paymentGateway) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.jobRepository = jobRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public Payment confirmPayment(Long userId, String paymentKey, String orderNumber, int amount) {
        // Find order
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."));

        // Verify ownership
        if (!order.getUserId().equals(userId)) {
            throw new PaymentException("FORBIDDEN", "해당 주문에 접근할 수 없습니다.");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new PaymentException("ALREADY_PAID", "이미 처리된 주문입니다.");
        }

        // Check amount
        if (order.getAmount() != amount) {
            throw new PaymentException("AMOUNT_MISMATCH", "결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // Check idempotency
        if (paymentRepository.existsByPaymentKey(paymentKey)) {
            throw new PaymentException("DUPLICATE", "이미 처리된 결제입니다.");
        }

        // Call payment gateway (stub in Phase 1)
        PaymentConfirmation confirmation = paymentGateway.confirm(paymentKey, orderNumber, amount);
        if (!confirmation.success()) {
            orderRepository.updateStatus(order.getId(), OrderStatus.FAILED);
            throw new PaymentException("PAYMENT_FAILED", confirmation.message());
        }

        // Save payment
        Payment payment = Payment.confirmed(order.getId(), confirmation.paymentKey(), confirmation.amount());
        paymentRepository.save(payment);

        // Update order status
        orderRepository.updateStatus(order.getId(), OrderStatus.PAID);

        // Enqueue registrar job only for paid orders (skip for free subdomains)
        if (order.getDomainId() != null) {
            String payload = "{\"domainId\":" + order.getDomainId() + ",\"orderId\":" + order.getId() + "}";
            RegistrarJob job = RegistrarJob.create(order.getDomainId(), JobType.REGISTER, payload);
            jobRepository.save(job);
        }

        return payment;
    }

    public static class PaymentException extends RuntimeException {
        private final String code;
        public PaymentException(String code, String message) { super(message); this.code = code; }
        public String getCode() { return code; }
    }
}
