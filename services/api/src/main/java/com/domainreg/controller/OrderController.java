package com.domainreg.controller;

import jakarta.validation.Valid;
import com.domainreg.core.entity.Order;
import com.domainreg.dto.CreateOrderRequest;
import com.domainreg.dto.CreateOrderResponse;
import com.domainreg.security.UserPrincipal;
import com.domainreg.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Order order = orderService.createOrder(principal.getUserId(),
            request.platformDomainId(), request.prefix());

        var paymentInfo = new CreateOrderResponse.PaymentInfo(
            "stub_ck_test",  // Toss client key (stub)
            order.getOrderNumber(),
            "서브도메인 발급: " + request.prefix(),
            order.getAmount(),
            principal.getEmail()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateOrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getAmount(),
            order.getCurrency(),
            order.getStatus().name(),
            paymentInfo
        ));
    }
}
