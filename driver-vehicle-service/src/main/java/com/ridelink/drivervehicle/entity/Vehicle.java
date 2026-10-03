package com.ridelink.drivervehicle.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "plate_number", nullable = false, unique = true, length = 50)
    private String plateNumber;
}
