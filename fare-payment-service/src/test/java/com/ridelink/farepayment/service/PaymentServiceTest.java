package com.ridelink.farepayment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ridelink.farepayment.client.RideServiceClient;
import com.ridelink.farepayment.client.RideServiceRide;
import com.ridelink.farepayment.domain.FareCalculator;
import com.ridelink.farepayment.dto.PaymentRequest;
import com.ridelink.farepayment.error.NotFoundError;
import com.ridelink.farepayment.error.DuplicatePaymentError;
import com.ridelink.farepayment.error.UnauthorizedError;
import com.ridelink.farepayment.error.ValidationError;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.repository.port.FareEstimateRepository;
import com.ridelink.farepayment.repository.port.PaymentRepository;
import com.ridelink.farepayment.messaging.MessagingClient;
import com.ridelink.farepayment.messaging.PaymentCompletedEvent;
import com.ridelink.farepayment.messaging.RideCompletedEvent;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock FareEstimateRepository estimates;
    @Mock RideServiceClient rideClient;
    @Mock PaymentOutcomeSimulator outcomeSimulator;
    @Mock MessagingClient messaging;
    private PaymentService service;
    private UUID rideId;

    @BeforeEach
    void setUp() {
        var fareService = new FareService(new FareCalculator(), estimates);
        service = new PaymentService(payments, rideClient, fareService, outcomeSimulator, messaging);
        rideId = UUID.randomUUID();
    }

    @Test
    void processesSuccessfulPayment() {
        when(rideClient.getRide(rideId, "Bearer test")).thenReturn(Optional.of(completedRide()));
        when(payments.findByRideId(rideId)).thenReturn(List.of());
        when(outcomeSimulator.succeeds()).thenReturn(true);
        when(payments.create(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.processPayment(request(rideId.toString()), "Bearer test");

        assertEquals(PaymentStatus.SUCCESS, result.status());
        assertEquals(70_000, result.amount());
        assertEquals(null, result.failureReason());
        verify(payments).create(any(Payment.class));
        ArgumentCaptor<PaymentCompletedEvent> event = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(messaging).publish(eq("payment.completed"), event.capture());
        assertEquals(result.id(), event.getValue().paymentId());
        assertEquals(rideId, event.getValue().rideId());
        assertEquals("1001", event.getValue().passengerId());
        assertEquals(70_000, event.getValue().amount());
    }

    @Test
    void persistsDeclinedPaymentAndReturnsFailureReason() {
        when(rideClient.getRide(rideId, "Bearer test")).thenReturn(Optional.of(completedRide()));
        when(payments.findByRideId(rideId)).thenReturn(List.of());
        when(outcomeSimulator.succeeds()).thenReturn(false);
        when(payments.create(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.processPayment(request(rideId.toString()), "Bearer test");

        assertEquals(PaymentStatus.FAILED, result.status());
        assertEquals("Simulated payment was declined", result.failureReason());
    }

    @Test
    void rejectsMalformedOrUnknownRide() {
        assertThrows(ValidationError.class, () -> service.processPayment(request("not-a-uuid"), "Bearer test"));
        when(rideClient.getRide(rideId, "Bearer test")).thenReturn(Optional.empty());
        assertThrows(NotFoundError.class, () -> service.processPayment(request(rideId.toString()), "Bearer test"));
    }

    @Test
    void rejectsRideThatIsNotCompleted() {
        when(rideClient.getRide(rideId, "Bearer test"))
            .thenReturn(Optional.of(new RideServiceRide(rideId, "IN_PROGRESS", "1001", "2001")));
        assertThrows(com.ridelink.farepayment.error.RideNotCompletedError.class,
            () -> service.processPayment(request(rideId.toString()), "Bearer test"));
    }

    @Test
    void rejectsDuplicateSuccessfulPayment() {
        when(rideClient.getRide(rideId, "Bearer test")).thenReturn(Optional.of(completedRide()));
        Payment existing = successfulPayment();
        existing.setRideId(rideId);
        when(payments.findByRideId(rideId)).thenReturn(List.of(existing));

        assertThrows(DuplicatePaymentError.class,
            () -> service.processPayment(request(rideId.toString()), "Bearer test"));
    }

    @Test
    void transactionReferenceCanForceDeterministicDecline() {
        when(rideClient.getRide(rideId, "Bearer test")).thenReturn(Optional.of(completedRide()));
        when(payments.findByRideId(rideId)).thenReturn(List.of());
        when(payments.create(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest forcedFailure = new PaymentRequest(rideId.toString(), null, "1001", "2001",
            5, 10, null, PaymentMethod.SIMULATED_CARD, "test-0000");
        var result = service.processPayment(forcedFailure, "Bearer test");

        assertEquals(PaymentStatus.FAILED, result.status());
        assertEquals("Simulated card decline: transaction reference ends with 0000", result.failureReason());
    }

    @Test
    void refundRequiresAdminAndUpdatesSuccessfulPayment() {
        Payment payment = successfulPayment();
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        doAnswer(invocation -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            return true;
        }).when(payments).updateStatus(payment.getId(), PaymentStatus.REFUNDED, null, "customer request");

        assertThrows(UnauthorizedError.class,
            () -> service.refundPayment(payment.getId(), "customer request", "ROLE_PASSENGER"));
        assertEquals(PaymentStatus.REFUNDED,
            service.refundPayment(payment.getId(), "customer request", "ROLE_ADMIN").status());
    }

    @Test
    void rejectsNonPositiveDistanceBeforeRideCall() {
        var invalid = new PaymentRequest(rideId.toString(), null, "1001", "2001",
            0, 5, null, PaymentMethod.CASH, "txn-1");
        assertThrows(ValidationError.class, () -> service.processPayment(invalid, "Bearer test"));
    }

    @Test
    void duplicateRideCompletionEventCreatesOnlyOnePayment() {
        String eventId = UUID.randomUUID().toString();
        var event = new RideCompletedEvent(eventId, "ride.completed", "1.0",
            java.time.Instant.parse("2026-10-03T12:00:00Z"), rideId.toString(), "1001", "2001",
            new java.math.BigDecimal("5.000"), 10, new java.math.BigDecimal("500.00"), "LKR");
        Payment saved = successfulPayment();
        when(payments.findByRideId(rideId)).thenReturn(List.of(), List.of(saved));
        when(payments.create(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outcomeSimulator.succeeds()).thenReturn(true);

        service.processRideCompleted(event);
        service.processRideCompleted(event);

        verify(payments).create(any(Payment.class));
    }

    private PaymentRequest request(String id) {
        return new PaymentRequest(id, null, "1001", "2001", 5, 10,
            null, PaymentMethod.SIMULATED_CARD, "txn-" + UUID.randomUUID());
    }

    private RideServiceRide completedRide() { return new RideServiceRide(rideId, "COMPLETED", "1001", "2001"); }

    private Payment successfulPayment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setRideId(rideId);
        payment.setPassengerId("1001");
        payment.setDriverId("2001");
        payment.setAmount(40_000);
        payment.setCurrency("LKR");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setTransactionRef("txn-refund");
        return payment;
    }
}
