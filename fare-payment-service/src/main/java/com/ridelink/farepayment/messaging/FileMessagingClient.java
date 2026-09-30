package com.ridelink.farepayment.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;

/**
 * Demo async adapter: appends events as JSON Lines and dispatches to in-process
 * subscribers on a daemon worker. Replace with RabbitMQ/Kafka for multi-instance delivery.
 */
@Component
@ConditionalOnProperty(name = "message.broker.type", havingValue = "file", matchIfMissing = true)
public class FileMessagingClient implements MessagingClient {
    private static final Logger log = LoggerFactory.getLogger(FileMessagingClient.class);
    private final ObjectMapper mapper;
    private final Path eventLog;
    private final Object appendLock = new Object();
    private final Map<String, List<Consumer<String>>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService deliveryExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ridelink-event-dispatch");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService fileTailer = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ridelink-event-tail");
        thread.setDaemon(true);
        return thread;
    });
    private volatile long readOffset;

    public FileMessagingClient(ObjectMapper mapper, @Value("${message.broker.url}") String brokerUrl) {
        this.mapper = mapper;
        if (!brokerUrl.startsWith("file:")) {
            throw new IllegalArgumentException("MESSAGE_BROKER_URL must be a file: URI when MESSAGE_BROKER_TYPE=file");
        }
        this.eventLog = Path.of(java.net.URI.create(brokerUrl));
    }

    @PostConstruct
    void startTailer() {
        try {
            readOffset = Files.exists(eventLog) ? Files.size(eventLog) : 0;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize demo event log", ex);
        }
        fileTailer.scheduleWithFixedDelay(this::deliverNewLines, 100, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void publish(String topic, Object event) {
        try {
            String payload = mapper.writeValueAsString(event);
            String logEntry = mapper.writeValueAsString(new EventLogEntry(topic, Instant.now(), mapper.readTree(payload)));
            synchronized (appendLock) {
                Path parent = eventLog.toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(eventLog, logEntry + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not append event to demo log", ex);
        }
    }

    @Override
    public void subscribe(String topic, Consumer<String> jsonPayloadConsumer) {
        subscribers.computeIfAbsent(topic, ignored -> new CopyOnWriteArrayList<>()).add(jsonPayloadConsumer);
    }

    @PreDestroy
    void stopDispatcher() {
        fileTailer.shutdownNow();
        deliveryExecutor.shutdown();
    }

    private void deliverNewLines() {
        try {
            if (!Files.exists(eventLog)) return;
            byte[] allBytes = Files.readAllBytes(eventLog);
            int start = Math.toIntExact(readOffset);
            if (allBytes.length <= start) return;
            int end = allBytes.length;
            while (end > start && allBytes[end - 1] != '\n') end--;
            if (end == start) return; // Wait until the producer finishes the JSONL record.
            String additions = new String(Arrays.copyOfRange(allBytes, start, end), StandardCharsets.UTF_8);
            readOffset = end;
            for (String line : additions.split("\\R")) {
                if (!line.isBlank()) dispatchLoggedEvent(line);
            }
        } catch (Exception ex) {
            log.error("Unable to read demo event log", ex);
        }
    }

    private void dispatchLoggedEvent(String line) throws IOException {
        var envelope = mapper.readTree(line);
        String topic = envelope.path("topic").asText();
        if (topic.isBlank() || !envelope.has("payload")) return;
        String payload = mapper.writeValueAsString(envelope.get("payload"));
        for (Consumer<String> subscriber : subscribers.getOrDefault(topic, List.of())) {
            deliveryExecutor.execute(() -> {
                try { subscriber.accept(payload); }
                catch (RuntimeException ex) { log.error("Subscriber failed for topic {}", topic, ex); }
            });
        }
    }

    private record EventLogEntry(String topic, Instant timestamp, Object payload) { }
}
