package com.ridelink.farepayment.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID id;

    // Ride Management owns rideId. This service persists the identifier without a remote DB FK.
    @Column(name = "ride_id", nullable = false, columnDefinition = "binary(16)")
    private UUID rideId;

    @Column(name = "fare_estimate_id", columnDefinition = "binary(16)")
    private UUID fareEstimateId;

    @Column(name = "passenger_id", nullable = false, length = 100)
    private String passengerId;

    @Column(name = "driver_id", nullable = false, length = 100)
    private String driverId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "LKR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_ref", nullable = false, unique = true, length = 100)
    private String transactionRef;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Payment() { }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRideId() { return rideId; }
    public UUID getFareEstimateId() { return fareEstimateId; }
    public String getPassengerId() { return passengerId; }
    public String getDriverId() { return driverId; }
    public int getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getTransactionRef() { return transactionRef; }
    public String getFailureReason() { return failureReason; }
    public String getRefundReason() { return refundReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setRideId(UUID rideId) { this.rideId = rideId; }
    public void setFareEstimateId(UUID fareEstimateId) { this.fareEstimateId = fareEstimateId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public void setAmount(int amount) { this.amount = amount; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    @PrePersist
    void assignTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}
