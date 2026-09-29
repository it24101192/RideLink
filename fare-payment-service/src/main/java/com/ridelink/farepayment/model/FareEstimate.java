package com.ridelink.farepayment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fare_estimates")
public class FareEstimate {
    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "binary(16)")
    private UUID id;

    // Identifiers owned by other services are stored as values; no cross-service FK is created.
    @Column(name = "ride_request_id", columnDefinition = "binary(16)")
    private UUID rideRequestId;

    @Column(name = "passenger_id", nullable = false, length = 100)
    private String passengerId;

    @Column(name = "pickup_location", nullable = false, length = 500)
    private String pickupLocation;

    @Column(name = "destination_location", nullable = false, length = 500)
    private String destinationLocation;

    @Column(name = "distance_km", nullable = false, precision = 12, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "surge_multiplier", nullable = false, precision = 3, scale = 2)
    private BigDecimal surgeMultiplier;

    @Column(name = "estimated_fare", nullable = false)
    private int estimatedFare;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "breakdown", nullable = false, columnDefinition = "json")
    private Map<String, Object> breakdown;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public FareEstimate() { }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRideRequestId() { return rideRequestId; }
    public String getPassengerId() { return passengerId; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDestinationLocation() { return destinationLocation; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public int getDurationMinutes() { return durationMinutes; }
    public BigDecimal getSurgeMultiplier() { return surgeMultiplier; }
    public int getEstimatedFare() { return estimatedFare; }
    public Map<String, Object> getBreakdown() { return breakdown; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRideRequestId(UUID rideRequestId) { this.rideRequestId = rideRequestId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setSurgeMultiplier(BigDecimal surgeMultiplier) { this.surgeMultiplier = surgeMultiplier; }
    public void setEstimatedFare(int estimatedFare) { this.estimatedFare = estimatedFare; }
    public void setBreakdown(Map<String, Object> breakdown) { this.breakdown = breakdown; }

    @jakarta.persistence.PrePersist
    void assignCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
