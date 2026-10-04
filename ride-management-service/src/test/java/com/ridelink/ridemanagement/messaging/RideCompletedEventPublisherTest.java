package com.ridelink.ridemanagement.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.repository.RideOutboxEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

class RideCompletedEventPublisherTest {
    @Test
    void enqueuesVersionedCompletionEventForTransactionalPublishing() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        RideOutboxEventRepository outbox = mock(RideOutboxEventRepository.class);
        when(mapper.writeValueAsString(any())).thenReturn("{\"eventType\":\"ride.completed\",\"eventVersion\":\"1.0\"}");
        RideCompletedEventPublisher publisher = new RideCompletedEventPublisher(rabbit, mapper, outbox,
                "ridelink.events", "ride.completed");
        Ride ride = Ride.requested(UUID.randomUUID(), 6.9, 79.8, "Pickup", 6.91, 79.9, "Destination",
                "Colombo", new BigDecimal("5.000"), 10, new BigDecimal("500.00"));
        ride.setDriverId(UUID.randomUUID());
        ride.setFinalFare(new BigDecimal("500.00"));
        ride.setCompletedAt(Instant.parse("2026-10-03T12:00:00Z"));

        publisher.enqueue(ride);

        ArgumentCaptor<com.ridelink.ridemanagement.model.RideOutboxEvent> event =
                ArgumentCaptor.forClass(com.ridelink.ridemanagement.model.RideOutboxEvent.class);
        verify(outbox).save(event.capture());
        assertEquals("{\"eventType\":\"ride.completed\",\"eventVersion\":\"1.0\"}", event.getValue().getPayload());
        ArgumentCaptor<RideCompletedEvent> payload = ArgumentCaptor.forClass(RideCompletedEvent.class);
        verify(mapper).writeValueAsString(payload.capture());
        assertNotNull(payload.getValue().eventId());
        assertEquals(ride.getId().toString(), payload.getValue().rideId());
        assertEquals("ride.completed", payload.getValue().eventType());
        assertEquals("1.0", payload.getValue().eventVersion());
    }
}
