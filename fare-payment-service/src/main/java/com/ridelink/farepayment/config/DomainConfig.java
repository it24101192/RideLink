package com.ridelink.farepayment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ridelink.farepayment.domain.FareCalculator;

@Configuration
public class DomainConfig {
    @Bean
    FareCalculator fareCalculator() { return new FareCalculator(); }
}
