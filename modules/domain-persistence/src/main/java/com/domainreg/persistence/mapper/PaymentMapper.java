package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface PaymentMapper {
    Optional<Payment> findById(@Param("id") Long id);
    Optional<Payment> findByPaymentKey(@Param("paymentKey") String paymentKey);
    void insert(Payment payment);
    boolean existsByPaymentKey(@Param("paymentKey") String paymentKey);
}
