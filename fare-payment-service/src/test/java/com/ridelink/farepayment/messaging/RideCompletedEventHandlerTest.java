package com.ridelink.farepayment.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.farepayment.dto.FinalFareDto;
import com.ridelink.farepayment.domain.FareCalculator;
import com.ridelink.farepayment.service.FareService;

class RideCompletedEventHandlerTest {
    @SuppressWarnings("unchecked")
    @Test
    void subscribesAndCalculatesFinalFareForRideCompletedEvent() {
        MessagingClient messaging = mock(MessagingClient.class);
        FareService fareService = mock(FareService.class);
        UUID rideId = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");
        var finalFare = new FinalFareDto(rideId.toString(), new FareCalculator().calculate(
            new FareCalculator.Input(5, 10)));
        when(fareService.calculateFinalFare(eq(rideId), eq(5.0), eq(10), any())).thenReturn(finalFare);
        var handler = new RideCompletedEventHandler(messaging, new ObjectMapper(), fareService);

        handler.subscribeToRideCompletions();
        ArgumentCaptor<Consumer<String>> consumer = ArgumentCaptor.forClass(Consumer.class);
        verify(messaging).subscribe(eq("ride.completed"), consumer.capture());
        consumer.getValue().accept("{\"rideId\":\"" + rideId + "\",\"actualDistanceKm\":5.0,"
            + "\"actualDurationMinutes\":10,\"surgeMultiplier\":1}");

        verify(fareService).calculateFinalFare(rideId, 5.0, 10, BigDecimal.ONE);
        verify(messaging).publish(eq("fare.calculated"), any(FinalFareCalculatedEvent.class));
    }
}
