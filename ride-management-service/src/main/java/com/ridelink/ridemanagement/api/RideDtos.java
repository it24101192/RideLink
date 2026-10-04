package com.ridelink.ridemanagement.api;

import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RideStatus;
import com.ridelink.ridemanagement.model.RideStatusHistory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class RideDtos {
    private RideDtos() { }
    public record RideResponse(UUID id, UUID passengerId, UUID driverId, double pickupLat, double pickupLng,
        String pickupAddress, double destLat, double destLng, String destAddress, String serviceArea, RideStatus status,
        BigDecimal estimatedFare, BigDecimal finalFare, BigDecimal distanceKm, int durationMin,
        String cancellationReason, Instant createdAt, Instant updatedAt, Instant completedAt,
        List<StatusHistoryResponse> statusHistory) {
        public static RideResponse from(Ride r, List<RideStatusHistory> history) {
            return new RideResponse(r.getId(), r.getPassengerId(), r.getDriverId(), r.getPickupLat(), r.getPickupLng(),
                r.getPickupAddress(), r.getDestLat(), r.getDestLng(), r.getDestAddress(), r.getServiceArea(), r.getStatus(),
                r.getEstimatedFare(), r.getFinalFare(), r.getDistanceKm(), r.getDurationMin(), r.getCancellationReason(),
                r.getCreatedAt(), r.getUpdatedAt(), r.getCompletedAt(), history.stream().map(StatusHistoryResponse::from).toList());
        }
    }
    public record StatusHistoryResponse(UUID id, RideStatus fromStatus, RideStatus toStatus, UUID changedBy, Instant changedAt, String reason) {
        static StatusHistoryResponse from(RideStatusHistory h) { return new StatusHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getChangedAt(), h.getReason()); }
    }
    public record FareEstimateResponse(BigDecimal estimatedFare, BigDecimal distanceKm, int durationMin, String currency) { }
    public record ReceiptResponse(UUID rideId, BigDecimal amount, String method, String status,
                                  String transactionRef, LocalDateTime paidAt) { }
}
