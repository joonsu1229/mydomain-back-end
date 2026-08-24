package com.domainreg.persistence.repository;

import com.domainreg.core.entity.Order;
import com.domainreg.core.enums.OrderStatus;
import com.domainreg.core.port.OrderRepository;
import com.domainreg.persistence.mapper.OrderMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper mapper;

    public OrderRepositoryImpl(OrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            mapper.insert(order);
        }
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return mapper.findByOrderNumber(orderNumber);
    }

    @Override
    public void updateStatus(Long id, OrderStatus status) {
        mapper.updateStatus(id, status);
    }
}
