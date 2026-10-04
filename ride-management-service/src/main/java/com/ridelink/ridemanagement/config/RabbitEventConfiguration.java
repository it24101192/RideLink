package com.ridelink.ridemanagement.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitEventConfiguration {
    @Bean
    DirectExchange rideLinkEventsExchange(@Value("${ride.events.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }
}
