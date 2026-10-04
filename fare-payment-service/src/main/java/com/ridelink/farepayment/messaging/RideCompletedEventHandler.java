package com.ridelink.farepayment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.farepayment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RideCompletedEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RideCompletedEventHandler.class);
    private final ObjectMapper mapper;
    private final PaymentService paymentService;

    public RideCompletedEventHandler(ObjectMapper mapper, PaymentService paymentService) {
        this.mapper = mapper;
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = "${ride.events.queue}")
    public void handle(String jsonPayload) {
        try {
            RideCompletedEvent event = mapper.readValue(jsonPayload, RideCompletedEvent.class);
            paymentService.processRideCompleted(event);
        } catch (Exception exception) {
            LOGGER.error("Could not process ride.completed event; broker retry/dead-letter policy will handle it", exception);
            throw new IllegalStateException("Could not process ride.completed event", exception);
        }
    }
}
