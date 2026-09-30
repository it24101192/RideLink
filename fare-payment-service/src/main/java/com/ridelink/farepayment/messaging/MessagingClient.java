package com.ridelink.farepayment.messaging;

import java.util.function.Consumer;

/** Messaging port; implementations deliver JSON payloads asynchronously to subscribers. */
public interface MessagingClient {
    void publish(String topic, Object event);
    void subscribe(String topic, Consumer<String> jsonPayloadConsumer);
}
