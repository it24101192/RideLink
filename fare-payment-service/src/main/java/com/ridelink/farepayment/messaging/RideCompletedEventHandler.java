package com.ridelink.farepayment.messaging;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.farepayment.service.FareService;
import jakarta.annotation.PostConstruct;

@Component
public class RideCompletedEventHandler {
    private static final Logger log = LoggerFactory.getLogger(RideCompletedEventHandler.class);
    private final MessagingClient messaging;
    private final ObjectMapper mapper;
    private final FareService fareService;

    public RideCompletedEventHandler(MessagingClient messaging, ObjectMapper mapper, FareService fareService) {
        this.messaging = messaging;
        this.mapper = mapper;
        this.fareService = fareService;
    }

    @PostConstruct
    void subscribeToRideCompletions() {
        messaging.subscribe("ride.completed", this::handle);
    }

    void handle(String jsonPayload) {
        try {
            RideCompletedEvent event = mapper.readValue(jsonPayload, RideCompletedEvent.class);
            var finalFare = fareService.calculateFinalFare(UUID.fromString(event.rideId()),
                event.actualDistanceKm(), event.actualDurationMinutes(), event.surgeMultiplier());
            messaging.publish("fare.calculated", new FinalFareCalculatedEvent("fare.calculated", finalFare));
        } catch (Exception ex) {
            log.error("Unable to process ride.completed event", ex);
        }
    }
}
