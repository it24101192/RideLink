package com.ridelink.ridemanagement.messaging;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ridelink.ridemanagement.model.Ride;
import com.ridelink.ridemanagement.model.RideOutboxEvent;
import com.ridelink.ridemanagement.repository.RideOutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

/** Transactional outbox writer and RabbitMQ publisher for ride.completed. */
@Component
public class RideCompletedEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RideCompletedEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;
    private final RideOutboxEventRepository outbox;
    private final String exchange;
    private final String routingKey;

    public RideCompletedEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper mapper,
            RideOutboxEventRepository outbox, @Value("${ride.events.exchange}") String exchange,
            @Value("${ride.events.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.outbox = outbox;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Transactional
    public void enqueue(Ride ride) {
        UUID eventId = UUID.randomUUID();
        RideCompletedEvent event = new RideCompletedEvent(eventId.toString(), "ride.completed", "1.0",
                ride.getCompletedAt() == null ? Instant.now() : ride.getCompletedAt(), ride.getId().toString(),
                ride.getPassengerId().toString(), ride.getDriverId() == null ? null : ride.getDriverId().toString(),
                ride.getDistanceKm(), ride.getDurationMin(), ride.getFinalFare(), "LKR");
        outbox.save(new RideOutboxEvent(eventId, mapper.writeValueAsString(event), Instant.now()));
    }

    @Scheduled(fixedDelayString = "${ride.events.publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        for (RideOutboxEvent event : outbox.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                CorrelationData correlation = new CorrelationData(event.getId().toString());
                rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload(), correlation);
                var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.isAck() || correlation.getReturned() != null) {
                    throw new IllegalStateException(confirm.getReason() == null
                            ? "RabbitMQ did not route the ride.completed event" : confirm.getReason());
                }
                event.setPublishedAt(Instant.now());
                outbox.save(event);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while publishing ride.completed event {}", event.getId(), exception);
                return;
            } catch (Exception exception) {
                LOGGER.error("Could not publish ride.completed event {}; it will be retried", event.getId(), exception);
            }
        }
    }
}
