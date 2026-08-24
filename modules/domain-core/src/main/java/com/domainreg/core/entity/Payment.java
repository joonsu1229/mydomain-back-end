package com.domainreg.core.entity;

import java.time.Instant;

public class Payment {
    private Long id;
    private Long orderId;
    private String pgProvider;
    private String pgPaymentKey;
    private int amount;
    private String status;
    private String rawPayload;
    private Instant paidAt;

    public static Payment confirmed(Long orderId, String pgPaymentKey, int amount) {
        Payment p = new Payment();
        p.orderId = orderId;
        p.pgProvider = "TOSS";
        p.pgPaymentKey = pgPaymentKey;
        p.amount = amount;
        p.status = "CONFIRMED";
        p.paidAt = Instant.now();
        return p;
    }

    // -- getters / setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getPgProvider() { return pgProvider; }
    public void setPgProvider(String pgProvider) { this.pgProvider = pgProvider; }

    public String getPgPaymentKey() { return pgPaymentKey; }
    public void setPgPaymentKey(String pgPaymentKey) { this.pgPaymentKey = pgPaymentKey; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
