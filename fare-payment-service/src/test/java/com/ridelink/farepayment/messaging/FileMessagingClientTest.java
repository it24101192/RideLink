package com.ridelink.farepayment.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;

class FileMessagingClientTest {
    @TempDir Path tempDir;

    @Test
    void appendsAndAsynchronouslyDeliversPublishedPayload() throws Exception {
        Path log = tempDir.resolve("events.jsonl");
        FileMessagingClient client = new FileMessagingClient(new ObjectMapper().findAndRegisterModules(), log.toUri().toString());
        client.startTailer();
        @SuppressWarnings("unchecked") Consumer<String> subscriber = mock(Consumer.class);
        client.subscribe("payment.completed", subscriber);

        client.publish("payment.completed", Map.of("rideId", "ride-1", "status", "SUCCESS"));

        verify(subscriber, timeout(1000)).accept(argThat(payload ->
            payload.contains("\"rideId\":\"ride-1\"") && payload.contains("\"status\":\"SUCCESS\"")));
        org.junit.jupiter.api.Assertions.assertTrue(Files.readString(log).contains("payment.completed"));
        client.stopDispatcher();
    }
}
