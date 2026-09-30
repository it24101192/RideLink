package com.ridelink.farepayment.service;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Uses a 90% success probability unless deterministic test mode is enabled. */
@Component
public class RandomPaymentOutcomeSimulator implements PaymentOutcomeSimulator {
    private final boolean testMode;
    private final boolean testOutcomeSuccess;

    public RandomPaymentOutcomeSimulator(
            @Value("${payment.test-mode:false}") boolean testMode,
            @Value("${payment.test-outcome-success:true}") boolean testOutcomeSuccess) {
        this.testMode = testMode;
        this.testOutcomeSuccess = testOutcomeSuccess;
    }

    @Override
    public boolean succeeds() {
        return testMode ? testOutcomeSuccess : ThreadLocalRandom.current().nextDouble() < 0.90;
    }
}
