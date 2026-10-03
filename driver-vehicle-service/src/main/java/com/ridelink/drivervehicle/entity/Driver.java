package com.ridelink.drivervehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Account Service user id (UUID) of this driver. */
    @Column(name = "account_id", length = 36)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID accountId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "license_no", nullable = false, unique = true, length = 50)
    private String licenseNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AvailabilityStatus availability = AvailabilityStatus.AVAILABLE;

    @Column(name = "service_area", length = 100)
    private String serviceArea;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;
}
