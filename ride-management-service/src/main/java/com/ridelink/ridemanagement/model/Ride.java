package com.ridelink.ridemanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class Ride {
    @Id private UUID id;
    @Column(name = "passenger_id", nullable = false) private UUID passengerId;
    @Column(name = "driver_id") private UUID driverId;
    @Column(name = "pickup_lat", nullable = false) private double pickupLat;
    @Column(name = "pickup_lng", nullable = false) private double pickupLng;
    @Column(name = "pickup_address", nullable = false, length = 300) private String pickupAddress;
    @Column(name = "dest_lat", nullable = false) private double destLat;
    @Column(name = "dest_lng", nullable = false) private double destLng;
    @Column(name = "dest_address", nullable = false, length = 300) private String destAddress;
    @Column(name = "service_area", length = 100) private String serviceArea;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private RideStatus status;
    @Column(name = "estimated_fare", nullable = false, precision = 12, scale = 2) private BigDecimal estimatedFare;
    @Column(name = "final_fare", precision = 12, scale = 2) private BigDecimal finalFare;
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 3) private BigDecimal distanceKm;
    @Column(name = "duration_min", nullable = false) private int durationMin;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected Ride() { }

    public static Ride requested(UUID passengerId, double pickupLat, double pickupLng, String pickupAddress,
                                 double destLat, double destLng, String destAddress,
                                 String serviceArea, BigDecimal distanceKm, int durationMin, BigDecimal estimatedFare) {
        Ride ride = new Ride();
        ride.id = UUID.randomUUID(); ride.passengerId = passengerId;
        ride.pickupLat = pickupLat; ride.pickupLng = pickupLng; ride.pickupAddress = pickupAddress;
        ride.destLat = destLat; ride.destLng = destLng; ride.destAddress = destAddress;
        ride.serviceArea = serviceArea;
        ride.distanceKm = distanceKm; ride.durationMin = durationMin; ride.estimatedFare = estimatedFare;
        ride.status = RideStatus.REQUESTED;
        return ride;
    }

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getPassengerId() { return passengerId; }
    public UUID getDriverId() { return driverId; }
    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public String getPickupAddress() { return pickupAddress; }
    public double getDestLat() { return destLat; }
    public double getDestLng() { return destLng; }
    public String getDestAddress() { return destAddress; }
    public String getServiceArea() { return serviceArea; }
    public RideStatus getStatus() { return status; }
    public BigDecimal getEstimatedFare() { return estimatedFare; }
    public BigDecimal getFinalFare() { return finalFare; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public int getDurationMin() { return durationMin; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setStatus(RideStatus value) { status = value; }
    public void setDriverId(UUID value) { driverId = value; }
    public void setFinalFare(BigDecimal value) { finalFare = value; }
    public void setCompletedAt(Instant value) { completedAt = value; }
    public void setCancellationReason(String value) { cancellationReason = value; }
}
