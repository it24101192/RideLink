package com.ridelink.farepayment.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.ridelink.farepayment.domain.FareCalculator;
import com.ridelink.farepayment.dto.FareEstimateDto;
import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FinalFareDto;
import com.ridelink.farepayment.error.NotFoundError;
import com.ridelink.farepayment.error.ValidationError;
import com.ridelink.farepayment.model.FareEstimate;
import com.ridelink.farepayment.repository.port.FareEstimateRepository;

@Service
public class FareService {
    private static final BigDecimal DEFAULT_SURGE = BigDecimal.ONE;

    private final FareCalculator calculator;
    private final FareEstimateRepository estimates;

    public FareService(FareCalculator calculator, FareEstimateRepository estimates) {
        this.calculator = calculator;
        this.estimates = estimates;
    }

    public FareEstimateDto estimateFare(FareEstimateRequest request) {
        if (request == null) throw new ValidationError("fare estimate request is required");
        validateFareInput(request.distanceKm(), request.durationMinutes(), request.surgeMultiplier());
        if (request.passengerId() == null || request.passengerId().isBlank()) {
            throw new ValidationError("passengerId is required");
        }
        if (request.pickupLocation() == null || request.pickupLocation().isBlank()) {
            throw new ValidationError("pickupLocation is required");
        }
        if (request.destinationLocation() == null || request.destinationLocation().isBlank()) {
            throw new ValidationError("destinationLocation is required");
        }

        BigDecimal surge = request.surgeMultiplier() == null ? DEFAULT_SURGE : request.surgeMultiplier();
        FareCalculator.Fare fare = calculator.calculate(new FareCalculator.Input(
            request.distanceKm(), request.durationMinutes(), surge));
        FareEstimate entity = new FareEstimate();
        entity.setRideRequestId(request.rideRequestId());
        entity.setPassengerId(request.passengerId());
        entity.setPickupLocation(request.pickupLocation().trim());
        entity.setDestinationLocation(request.destinationLocation().trim());
        entity.setDistanceKm(BigDecimal.valueOf(request.distanceKm()));
        entity.setDurationMinutes(request.durationMinutes());
        entity.setSurgeMultiplier(surge);
        entity.setEstimatedFare(toStoredAmount(fare.totalFare()));
        entity.setBreakdown(breakdown(fare));
        return toDto(estimates.create(entity));
    }

    public FinalFareDto calculateFinalFare(UUID rideId, double actualDistanceKm,
                                            int actualDurationMinutes, BigDecimal surge) {
        if (rideId == null) throw new ValidationError("rideId is required");
        validateFareInput(actualDistanceKm, actualDurationMinutes, surge);
        FareCalculator.Fare fare = calculator.calculate(
            new FareCalculator.Input(actualDistanceKm, actualDurationMinutes, surge));
        return new FinalFareDto(rideId.toString(), fare);
    }

    public FareEstimateDto getEstimateById(UUID id) {
        if (id == null) throw new ValidationError("estimate id is required");
        return estimates.findById(id).map(FareService::toDto)
            .orElseThrow(() -> new NotFoundError("Fare estimate not found: " + id));
    }

    private static void validateFareInput(double distanceKm, int durationMinutes, BigDecimal surge) {
        if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
            throw new ValidationError("distanceKm must be a finite number greater than zero");
        }
        if (durationMinutes < 0) throw new ValidationError("durationMinutes must be non-negative");
        if (surge != null && (surge.compareTo(BigDecimal.ONE) < 0
                || surge.compareTo(new BigDecimal("2.5")) > 0)) {
            throw new ValidationError("surgeMultiplier must be between 1.0 and 2.5");
        }
    }

    static Map<String, Object> breakdown(FareCalculator.Fare fare) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseFare", fare.baseFare());
        result.put("distanceFare", fare.distanceFare());
        result.put("timeFare", fare.timeFare());
        result.put("surgeAmount", fare.surgeAmount());
        result.put("subtotal", fare.subtotal());
        result.put("minimumApplied", fare.minimumApplied());
        result.put("totalFare", fare.totalFare());
        result.put("currency", fare.currency());
        result.put("rule", fare.breakdown());
        return result;
    }

    static FareEstimateDto toDto(FareEstimate entity) {
        return new FareEstimateDto(entity.getId(), entity.getRideRequestId(), entity.getPassengerId(),
            entity.getPickupLocation(), entity.getDestinationLocation(), entity.getDistanceKm(),
            entity.getDurationMinutes(), entity.getSurgeMultiplier(), entity.getEstimatedFare(),
            entity.getBreakdown(), entity.getCreatedAt(), "LKR");
    }

    static int toStoredAmount(long cents) {
        try { return Math.toIntExact(cents); }
        catch (ArithmeticException ex) { throw new ValidationError("fare exceeds supported integer cents range"); }
    }
}
