package com.ridelink.ridemanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_status_history")
public class RideStatusHistory {
    @Id private UUID id;
    @Column(name = "ride_id", nullable = false) private UUID rideId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 24) private RideStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 24) private RideStatus toStatus;
    @Column(name = "changed_by", nullable = false) private UUID changedBy;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
    @Column(length = 500) private String reason;

    protected RideStatusHistory() { }
    public RideStatusHistory(UUID rideId, RideStatus fromStatus, RideStatus toStatus, UUID changedBy, String reason) {
        this.id = UUID.randomUUID(); this.rideId = rideId; this.fromStatus = fromStatus;
        this.toStatus = toStatus; this.changedBy = changedBy; this.reason = reason; this.changedAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getRideId() { return rideId; }
    public RideStatus getFromStatus() { return fromStatus; }
    public RideStatus getToStatus() { return toStatus; }
    public UUID getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
}
