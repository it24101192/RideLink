package com.ridelink.ridemanagement.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RideCompletedEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RideCompletedEventPublisher.class);
    private final ObjectMapper mapper;
    private final String brokerType;
    private final String brokerUrl;
    public RideCompletedEventPublisher(ObjectMapper mapper, @Value("${message.broker.type:file}") String brokerType,
                                       @Value("${message.broker.url:file:/tmp/ridelink-events.jsonl}") String brokerUrl) {
        this.mapper = mapper; this.brokerType = brokerType; this.brokerUrl = brokerUrl;
    }
    public void publish(UUID rideId, UUID passengerId, UUID driverId, java.math.BigDecimal finalFare, Instant completedAt) {
        if (!"file".equalsIgnoreCase(brokerType)) {
            LOGGER.warn("Unsupported message broker type {}; ride.completed was not published", brokerType); return;
        }
        String fileName = brokerUrl.startsWith("file:") ? brokerUrl.substring(5) : brokerUrl;
        try {
            Path path = Path.of(fileName); if (path.getParent() != null) Files.createDirectories(path.getParent());
            var event = new RideCompletedEvent(UUID.randomUUID().toString(), "ride.completed", rideId.toString(),
                passengerId.toString(), driverId == null ? null : driverId.toString(), finalFare, completedAt);
            Files.writeString(path, mapper.writeValueAsString(event) + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) { LOGGER.error("Could not publish ride.completed event for {}", rideId, ex); }
    }
    public record RideCompletedEvent(String eventId, String eventType, String rideId, String passengerId,
                                     String driverId, java.math.BigDecimal finalFare, Instant occurredAt) { }
}
