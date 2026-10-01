package com.ridelink.ridemanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_payments")
public class RidePayment {
    @Id private UUID id;
    @Column(name = "ride_id", nullable = false, unique = true) private UUID rideId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 24) private String method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PaymentStatus status;
    @Column(name = "transaction_ref", nullable = false, length = 100) private String transactionRef;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected RidePayment() { }
    public RidePayment(UUID rideId, BigDecimal amount, PaymentStatus status, String transactionRef) {
        this.id = UUID.randomUUID(); this.rideId = rideId; this.amount = amount; this.status = status;
        this.method = "SIMULATED"; this.transactionRef = transactionRef; this.createdAt = Instant.now();
        this.paidAt = status == PaymentStatus.PAID ? Instant.now() : null;
    }
    public UUID getId() { return id; }
    public UUID getRideId() { return rideId; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionRef() { return transactionRef; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
}
