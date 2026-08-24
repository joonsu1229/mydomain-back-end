package com.domainreg.persistence.repository;

import com.domainreg.core.entity.Payment;
import com.domainreg.core.port.PaymentRepository;
import com.domainreg.persistence.mapper.PaymentMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentMapper mapper;

    public PaymentRepositoryImpl(PaymentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            mapper.insert(payment);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Optional<Payment> findByPaymentKey(String paymentKey) {
        return mapper.findByPaymentKey(paymentKey);
    }

    @Override
    public boolean existsByPaymentKey(String paymentKey) {
        return mapper.existsByPaymentKey(paymentKey);
    }
}
