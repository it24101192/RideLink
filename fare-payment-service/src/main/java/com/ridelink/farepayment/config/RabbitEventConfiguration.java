package com.ridelink.farepayment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitEventConfiguration {
    @Bean
    DirectExchange rideLinkEventsExchange(@Value("${ride.events.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    DirectExchange rideLinkEventsDeadLetterExchange(@Value("${ride.events.dead-letter-exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    Queue rideCompletedQueue(@Value("${ride.events.queue}") String queue,
            @Value("${ride.events.dead-letter-exchange}") String deadLetterExchange,
            @Value("${ride.events.dead-letter-queue}") String deadLetterQueue) {
        return QueueBuilder.durable(queue).deadLetterExchange(deadLetterExchange)
                .deadLetterRoutingKey(deadLetterQueue).build();
    }

    @Bean
    Queue rideCompletedDeadLetterQueue(@Value("${ride.events.dead-letter-queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding rideCompletedBinding(@Qualifier("rideCompletedQueue") Queue rideCompletedQueue,
            @Qualifier("rideLinkEventsExchange") DirectExchange rideLinkEventsExchange,
            @Value("${ride.events.routing-key}") String routingKey) {
        return BindingBuilder.bind(rideCompletedQueue).to(rideLinkEventsExchange).with(routingKey);
    }

    @Bean
    Binding rideCompletedDeadLetterBinding(@Qualifier("rideCompletedDeadLetterQueue") Queue rideCompletedDeadLetterQueue,
            @Qualifier("rideLinkEventsDeadLetterExchange") DirectExchange rideLinkEventsDeadLetterExchange,
            @Value("${ride.events.dead-letter-queue}") String routingKey) {
        return BindingBuilder.bind(rideCompletedDeadLetterQueue).to(rideLinkEventsDeadLetterExchange).with(routingKey);
    }
}
