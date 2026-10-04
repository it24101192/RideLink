package com.ridelink.farepayment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Publishes domain events to the shared RideLink topic exchange. */
@Component
public class RabbitMessagingClient implements MessagingClient {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper;
    private final String exchange;

    public RabbitMessagingClient(RabbitTemplate rabbitTemplate, ObjectMapper mapper,
            @Value("${ride.events.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper = mapper;
        this.exchange = exchange;
    }

    @Override
    public void publish(String topic, Object event) {
        try {
            rabbitTemplate.convertAndSend(exchange, topic, mapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize event " + topic, exception);
        }
    }
}
