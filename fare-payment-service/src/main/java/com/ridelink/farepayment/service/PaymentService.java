package com.ridelink.farepayment.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ridelink.farepayment.client.RideServiceClient;
import com.ridelink.farepayment.domain.FareCalculator;
import com.ridelink.farepayment.dto.FinalFareDto;
import com.ridelink.farepayment.dto.PaymentDto;
import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.dto.ReceiptDto;
import com.ridelink.farepayment.error.NotFoundError;
import com.ridelink.farepayment.error.DuplicatePaymentError;
import com.ridelink.farepayment.error.RideNotCompletedError;
import com.ridelink.farepayment.error.UnauthorizedError;
import com.ridelink.farepayment.error.ValidationError;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.messaging.MessagingClient;
import com.ridelink.farepayment.messaging.PaymentCompletedEvent;
import com.ridelink.farepayment.messaging.RideCompletedEvent;
import com.ridelink.farepayment.repository.port.PaymentRepository;

@Service
public class PaymentService {
    private static final String FAILURE_REASON = "Simulated payment was declined";

    private final PaymentRepository payments;
    private final RideServiceClient rideClient;
    private final FareService fareService;
    private final PaymentOutcomeSimulator outcomeSimulator;
    private final MessagingClient messaging;

    public PaymentService(PaymentRepository payments, RideServiceClient rideClient,
                          FareService fareService, PaymentOutcomeSimulator outcomeSimulator,
                          MessagingClient messaging) {
        this.payments = payments;
        this.rideClient = rideClient;
        this.fareService = fareService;
        this.outcomeSimulator = outcomeSimulator;
        this.messaging = messaging;
    }

    @Transactional
    public PaymentDto processPayment(PaymentRequest request, String bearerToken) {
        if (request == null) throw new ValidationError("payment request is required");
        UUID rideId = parseRideId(request.rideId());
        if (request.passengerId() == null || request.passengerId().isBlank()
                || request.driverId() == null || request.driverId().isBlank()) {
            throw new ValidationError("passengerId and driverId are required");
        }
        if (request.paymentMethod() == null) throw new ValidationError("paymentMethod is required");
        if (request.transactionRef() == null || request.transactionRef().isBlank()) {
            throw new ValidationError("transactionRef is required");
        }

        // Calculate first so invalid fare input is rejected before making a remote call.
        FinalFareDto finalFare = fareService.calculateFinalFare(rideId,
            request.actualDistanceKm(), request.actualDurationMinutes(), request.surgeMultiplier());

        var ride = rideClient.getRide(rideId, bearerToken)
            .orElseThrow(() -> new NotFoundError("Ride not found: " + rideId));
        if (!ride.isCompleted()) {
            throw new RideNotCompletedError("Ride must be in COMPLETED state before payment");
        }
        if (!request.passengerId().equals(ride.passengerId()) || !request.driverId().equals(ride.driverId())) {
            throw new UnauthorizedError("Payment passenger and driver IDs must match the ride");
        }
        if (!payments.findByRideId(rideId).isEmpty()) {
            throw new DuplicatePaymentError("A payment record already exists for this ride");
        }

        int amount = FareService.toStoredAmount(finalFare.fare().totalFare());
        if (amount <= 0) throw new ValidationError("amount must be positive");

        boolean forcedFailure = request.transactionRef().trim().endsWith("0000");
        boolean successful = !forcedFailure && outcomeSimulator.succeeds();
        Payment payment = new Payment();
        payment.setRideId(rideId);
        payment.setFareEstimateId(request.fareEstimateId());
        payment.setPassengerId(request.passengerId());
        payment.setDriverId(request.driverId());
        payment.setAmount(amount);
        payment.setCurrency("LKR");
        payment.setStatus(successful ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setTransactionRef(request.transactionRef().trim());
        payment.setFailureReason(successful ? null : forcedFailure
            ? "Simulated card decline: transaction reference ends with 0000" : FAILURE_REASON);
        Payment saved = payments.create(payment);
        PaymentDto result = toDto(saved);
        messaging.publish("payment.completed", new PaymentCompletedEvent(saved.getId(), saved.getRideId(),
            saved.getPassengerId(), saved.getDriverId(), saved.getAmount(), saved.getCurrency(),
            saved.getStatus(), java.time.Instant.now()));
        return result;
    }

    /** Processes a trusted, committed ride.completed event exactly once per ride. */
    @Transactional
    public PaymentDto processRideCompleted(RideCompletedEvent event) {
        if (event == null || !"ride.completed".equals(event.eventType()) || !"1.0".equals(event.eventVersion())
                || !isUuid(event.eventId()) || event.occurredAt() == null) {
            throw new ValidationError("Unsupported or malformed ride.completed event");
        }
        UUID rideId = parseRideId(event.rideId());
        if (event.passengerId() == null || event.driverId() == null || event.fareAmount() == null
                || event.fareAmount().signum() <= 0 || !"LKR".equalsIgnoreCase(event.currency())) {
            throw new ValidationError("ride.completed event is missing payment details");
        }
        if (event.distanceKm() == null || !Double.isFinite(event.distanceKm().doubleValue())
                || event.distanceKm().signum() <= 0 || event.durationMinutes() < 0) {
            throw new ValidationError("ride.completed event contains invalid trip metrics");
        }
        var existing = payments.findByRideId(rideId);
        if (!existing.isEmpty()) return toDto(existing.getFirst());

        long cents;
        try {
            cents = event.fareAmount().movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException ex) {
            throw new ValidationError("ride.completed fare is outside the supported amount range");
        }
        int amount = FareService.toStoredAmount(cents);
        boolean successful = outcomeSimulator.succeeds();
        Payment payment = new Payment();
        payment.setRideId(rideId);
        payment.setPassengerId(event.passengerId());
        payment.setDriverId(event.driverId());
        payment.setAmount(amount);
        payment.setCurrency("LKR");
        payment.setPaymentMethod(com.ridelink.farepayment.model.PaymentMethod.CASH);
        payment.setTransactionRef("RIDE-" + event.eventId());
        payment.setStatus(successful ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setFailureReason(successful ? null : FAILURE_REASON);
        Payment saved = payments.create(payment);
        PaymentDto result = toDto(saved);
        messaging.publish("payment.completed", new PaymentCompletedEvent(saved.getId(), saved.getRideId(),
            saved.getPassengerId(), saved.getDriverId(), saved.getAmount(), saved.getCurrency(),
            saved.getStatus(), java.time.Instant.now()));
        return result;
    }

    public PaymentDto getPaymentById(UUID id) {
        return findPayment(id);
    }

    public List<PaymentDto> getPaymentByRideId(UUID rideId, String bearerToken) {
        if (rideId == null) throw new ValidationError("rideId is required");
        if (rideClient.getRide(rideId, bearerToken).isEmpty()) throw new NotFoundError("Ride not found: " + rideId);
        return payments.findByRideId(rideId).stream().map(PaymentService::toDto).toList();
    }

    public ReceiptDto getReceiptByPaymentId(UUID id) {
        Payment payment = findEntity(id);
        return new ReceiptDto(payment.getId(), payment.getRideId(), payment.getPassengerId(),
            payment.getDriverId(), payment.getAmount(), payment.getCurrency(), payment.getStatus(),
            payment.getPaymentMethod(), payment.getTransactionRef(), payment.getCreatedAt());
    }

    @Transactional
    public PaymentDto refundPayment(UUID id, String reason, String actorRole) {
        if (!"ADMIN".equals(actorRole) && !"ROLE_ADMIN".equals(actorRole)) {
            throw new UnauthorizedError("Only an admin can refund payments");
        }
        if (reason == null || reason.isBlank()) throw new ValidationError("refund reason is required");
        Payment payment = findEntity(id);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ValidationError("Only successful payments can be refunded");
        }
        if (!payments.updateStatus(id, PaymentStatus.REFUNDED, null, reason.trim())) {
            throw new NotFoundError("Payment not found: " + id);
        }
        return findPayment(id);
    }

    private PaymentDto findPayment(UUID id) { return toDto(findEntity(id)); }

    private Payment findEntity(UUID id) {
        if (id == null) throw new ValidationError("payment id is required");
        return payments.findById(id).orElseThrow(() -> new NotFoundError("Payment not found: " + id));
    }

    private static UUID parseRideId(String rideId) {
        try { return UUID.fromString(rideId); }
        catch (RuntimeException ex) { throw new ValidationError("rideId must be a valid UUID"); }
    }

    private static boolean isUuid(String value) {
        try { UUID.fromString(value); return true; }
        catch (RuntimeException exception) { return false; }
    }

    private static PaymentDto toDto(Payment payment) {
        return new PaymentDto(payment.getId(), payment.getRideId(), payment.getFareEstimateId(),
            payment.getPassengerId(), payment.getDriverId(), payment.getAmount(), payment.getCurrency(),
            payment.getStatus(), payment.getPaymentMethod(), payment.getTransactionRef(),
            payment.getFailureReason(), payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
