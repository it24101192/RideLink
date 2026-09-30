package com.ridelink.farepayment.service;

@FunctionalInterface
public interface PaymentOutcomeSimulator {
    boolean succeeds();
}
