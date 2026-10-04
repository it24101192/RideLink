package com.ridelink.farepayment.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.ridelink.farepayment.config.RestClientConfig;
import com.ridelink.farepayment.service.PaymentService;

class RideCompletedEventHandlerTest {
    @Test
    void consumesVersionedRideCompletedEventAndStartsPaymentProcessing() throws Exception {
        PaymentService paymentService = mock(PaymentService.class);
        UUID rideId = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");
        var event = new RideCompletedEvent(UUID.randomUUID().toString(), "ride.completed", "1.0",
                Instant.parse("2026-10-03T12:00:00Z"), rideId.toString(), "passenger", "driver",
                new BigDecimal("5.000"), 10, new BigDecimal("500.00"), "LKR");
        var mapper = new RestClientConfig().objectMapper();
        var handler = new RideCompletedEventHandler(mapper, paymentService);

        handler.handle(mapper.writeValueAsString(event));

        verify(paymentService).processRideCompleted(event);
    }
}
