package com.domainreg.core.port;

import com.domainreg.core.entity.Order;
import com.domainreg.core.enums.OrderStatus;

import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    Optional<Order> findByOrderNumber(String orderNumber);
    void updateStatus(Long id, OrderStatus status);
}
