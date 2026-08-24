package com.domainreg.persistence.mapper;

import com.domainreg.core.entity.Order;
import com.domainreg.core.enums.OrderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface OrderMapper {
    Optional<Order> findById(@Param("id") Long id);
    Optional<Order> findByOrderNumber(@Param("orderNumber") String orderNumber);
    void insert(Order order);
    void updateStatus(@Param("id") Long id, @Param("status") OrderStatus status);
}
