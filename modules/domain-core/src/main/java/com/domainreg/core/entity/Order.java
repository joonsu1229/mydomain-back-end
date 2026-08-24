package com.domainreg.core.entity;

import com.domainreg.core.enums.OrderStatus;
import com.domainreg.core.enums.ProductType;
import java.time.Instant;

public class Order {
    private Long id;
    private Long userId;
    private Long domainId;
    private String orderNumber;
    private int amount;
    private String currency;
    private ProductType productType;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static Order create(Long userId, Long domainId, String orderNumber,
                                int amount, ProductType productType) {
        Order o = new Order();
        o.userId = userId;
        o.domainId = domainId;
        o.orderNumber = orderNumber;
        o.amount = amount;
        o.currency = "KRW";
        o.productType = productType;
        o.status = OrderStatus.PENDING;
        return o;
    }

    // -- getters / setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDomainId() { return domainId; }
    public void setDomainId(Long domainId) { this.domainId = domainId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
