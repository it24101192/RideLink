package com.ridelink.ridemanagement.entity;

import com.ridelink.ridemanagement.enums.RideStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long passengerId;

    // Nullable until a driver is assigned
    private Long driverId;

    @Column(nullable = false)
    private String pickupLocation;

    @Column(nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    private Double estimatedFare;

    private Double finalFare;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RideStatus.REQUESTED;
        }
    }
}
